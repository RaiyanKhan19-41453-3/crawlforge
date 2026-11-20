package com.crawlforge.scraper.http;

import com.crawlforge.scraper.core.config.RetryPolicy;
import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.exception.AntiBotException;
import com.crawlforge.scraper.core.exception.CaptchaDetectedException;
import com.crawlforge.scraper.core.exception.FetchException;
import com.crawlforge.scraper.core.exception.RateLimitException;
import com.crawlforge.scraper.core.exception.NonRetryableFetchException;
import com.crawlforge.scraper.core.exception.RetryExhaustedException;
import com.crawlforge.scraper.orchestration.MetricsCollector;
import com.crawlforge.scraper.stealth.AntiBotDetector;
import com.crawlforge.scraper.stealth.BrowserFingerprint;
import com.crawlforge.scraper.stealth.CaptchaSolver;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.protocol.RedirectLocations;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP transport with connection pooling, rate limiting, proxy/UA rotation, cookie sessions,
 * typed exceptions, retries with backoff and anti-bot detection. This is the single place where
 * raw network I/O happens.
 */
public final class ResilientHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ResilientHttpClient.class);
    private static final int MAX_BODY_BYTES = 50 * 1024 * 1024;
    private static final Pattern CONTENT_TYPE_CHARSET =
            Pattern.compile("(?i)charset\\s*=\\s*['\"]?([\\w.-]+)");
    private static final Pattern META_CHARSET =
            Pattern.compile("(?i)<meta[^>]+charset\\s*=\\s*['\"]?([\\w.-]+)");

    private record RawResponse(int status, byte[] body, String contentType,
                               String url, String finalUrl, Map<String, String> headers) { }

    private final ScraperConfig config;
    private final CloseableHttpClient client;
    private final CookieJar cookies = new CookieJar();
    private final RateLimiter rateLimiter;
    private final RetryPolicy retryPolicy;
    private final UserAgentRotator userAgents;
    private final ProxyRotator proxies;
    private final BasicCredentialsProvider credentials = new BasicCredentialsProvider();
    private final AntiBotDetector antiBotDetector = new AntiBotDetector();
    private final CaptchaSolver captchaSolver;
    private final BrowserFingerprint fingerprint;

    private final AtomicInteger totalRetries = new AtomicInteger();
    private final AtomicInteger captchaChallenges = new AtomicInteger();
    private final AtomicInteger blockedResponses = new AtomicInteger();
    private volatile MetricsCollector metrics;
    private final AtomicBoolean authenticated = new AtomicBoolean();

    public ResilientHttpClient(ScraperConfig config) {
        this(config, UserAgentRotator.DEFAULTS, CaptchaSolver.QUEUE, BrowserFingerprint.consistent());
    }

    public ResilientHttpClient(ScraperConfig config, java.util.List<String> userAgents,
                               CaptchaSolver captchaSolver, BrowserFingerprint fingerprint) {
        this.config = config;
        this.rateLimiter = new RateLimiter(config.requestsPerSecond());
        this.retryPolicy = config.retryPolicy();
        this.userAgents = new UserAgentRotator(userAgents);
        this.proxies = new ProxyRotator(config.proxies());
        this.captchaSolver = captchaSolver;
        this.fingerprint = fingerprint;
        this.client = buildClient();
    }

    private CloseableHttpClient buildClient() {
        proxies.applyCredentialsTo(credentials);
        return HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(config.maxThreads() * 4)
                        .setMaxConnPerRoute(config.maxThreads() * 2)
                        .build())
                .setDefaultCookieStore(cookies.delegate())
                .setDefaultCredentialsProvider(credentials)
                .disableAutomaticRetries() // handled explicitly so we can log and back off
                .evictIdleConnections(org.apache.hc.core5.util.TimeValue.ofSeconds(30))
                .build();
    }

    @FunctionalInterface
    private interface RequestAction<T> {
        T run(org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) throws IOException;
    }

    /** Shared retry/backoff/rate-limit shell for text and binary fetches. */
    private <T> T withRetries(String url, RequestAction<T> action) {
        Exception last = null;
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                rateLimiter.acquire();
                HttpGet get = new HttpGet(URI.create(url));
                get.setConfig(requestConfig());
                applyStealthHeaders(get, url);
                return action.run(get);
            } catch (CaptchaDetectedException | NonRetryableFetchException e) {
                // Permanent failures must not burn retries or inflate transport counters.
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FetchException("Fetch interrupted for " + url, e);
            } catch (Exception e) {
                last = e;
                if (!retryPolicy.shouldRetry(attempt)) break;
                totalRetries.incrementAndGet();
                if (metrics != null) metrics.addRetries(1);
                log.warn("Attempt {}/{} failed for {}: {} - retrying in {}", attempt,
                        retryPolicy.maxAttempts(), url, e.getMessage(), retryPolicy.delayFor(attempt));
                sleep(retryPolicy.delayFor(attempt).toMillis());
            }
        }
        throw new RetryExhaustedException("Fetch failed after " + retryPolicy.maxAttempts()
                + " attempts: " + url, last);
    }

    /** GET a URL as text with retries, backoff, charset detection and anti-bot handling. */
    public HttpResponse fetch(String url) {
        return withRetries(url, request -> {
            RawResponse raw = execute(request, url);
            HttpResponse response = new HttpResponse(raw.status(), decode(raw.body(), raw.contentType()),
                    raw.url(), raw.finalUrl(), raw.headers());
            return validate(response);
        });
    }

    /** GET a URL as raw bytes; never decodes binary formats such as PDF. */
    public BinaryResponse fetchBytes(String url) {
        return withRetries(url, request -> {
            RawResponse raw = execute(request, url);
            validateBinaryStatus(raw, url);
            return new BinaryResponse(raw.body());
        });
    }

    private void validateBinaryStatus(RawResponse raw, String url) {
        if (raw.status() == 429 || raw.status() >= 500) {
            blockedResponses.incrementAndGet();
            if (raw.status() == 429 && metrics != null) metrics.addBlocked(1);
            throw new FetchException("HTTP " + raw.status() + " from " + url);
        }
        if (raw.status() >= 400) {
            throw new NonRetryableFetchException("HTTP " + raw.status() + " from " + url);
        }
    }

    /** Form POST used by login flows. */
    public HttpResponse postForm(String url, Map<String, String> form) {
        try {
            rateLimiter.acquire();
            HttpPost post = new HttpPost(URI.create(url));
            post.setConfig(requestConfig());
            applyStealthHeaders(post, url);
            var pairs = form.entrySet().stream()
                    .map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
                    .toList();
            post.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
            RawResponse raw = execute(post, url);
            return validate(new HttpResponse(raw.status(), decode(raw.body(), raw.contentType()),
                    raw.url(), raw.finalUrl(), raw.headers()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("POST interrupted for " + url, e);
        } catch (java.io.IOException e) {
            throw new FetchException("POST failed for " + url, e);
        }
    }

    private RawResponse execute(org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                                String url) throws IOException {
        HttpClientContext context = HttpClientContext.create();
        return client.execute(request, context, response -> {
            int status = response.getCode();
            if (status == 304) throw new FetchException("HTTP 304 (not modified) from " + url);

            Map<String, String> headers = new LinkedHashMap<>();
            for (Header header : response.getHeaders()) headers.put(header.getName(), header.getValue());
            Header length = response.getHeader("Content-Length");
            if (length != null) {
                try {
                    if (Long.parseLong(length.getValue()) > MAX_BODY_BYTES) {
                        throw new FetchException("Response too large from " + url);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore an invalid Content-Length; the actual byte count is checked below.
                }
            }
            byte[] body = response.getEntity() == null
                    ? new byte[0] : response.getEntity().getContent().readAllBytes();
            if (body.length > MAX_BODY_BYTES) throw new FetchException("Response too large from " + url);

            String finalUrl = url;
            RedirectLocations redirects = context.getRedirectLocations();
            if (redirects != null && redirects.size() > 0) {
                finalUrl = redirects.getAll().get(redirects.size() - 1).toString();
            }
            return new RawResponse(status, body, response.getHeader("Content-Type") == null ? null
                    : response.getHeader("Content-Type").getValue(), url, finalUrl, headers);
        });
    }

    private HttpResponse validate(HttpResponse response) {
        if (response.status() == 429) {
            blockedResponses.incrementAndGet();
            if (metrics != null) metrics.addBlocked(1);
            throw new RateLimitException("HTTP 429 from " + response.url());
        }
        if (response.status() >= 500) {
            throw new FetchException("HTTP " + response.status() + " from " + response.url());
        }
        if (response.status() >= 400) {
            throw new NonRetryableFetchException("HTTP " + response.status() + " from " + response.url());
        }
        AntiBotDetector.ThreatLevel threat = antiBotDetector.detect(response.body(), response.status());
        if (threat == AntiBotDetector.ThreatLevel.CAPTCHA) {
            captchaChallenges.incrementAndGet();
            if (metrics != null) metrics.addCaptchas(1);
            if (captchaSolver.solve(response.url(), response.body())) {
                throw new FetchException("CAPTCHA solved, retrying " + response.url());
            }
            throw new CaptchaDetectedException("CAPTCHA challenge at " + response.url());
        }
        if (threat == AntiBotDetector.ThreatLevel.BLOCKED) {
            blockedResponses.incrementAndGet();
            if (metrics != null) metrics.addBlocked(1);
            throw new AntiBotException("Bot challenge from " + response.url());
        }
        return response;
    }

    /** Charset from Content-Type, else from an HTML meta tag in the first 2 KB, else UTF-8. */
    public static String decode(byte[] body, String contentType) {
        if (contentType != null) {
            Matcher matcher = CONTENT_TYPE_CHARSET.matcher(contentType);
            if (matcher.find()) return decodeWith(body, matcher.group(1));
        }
        int headLength = Math.min(body.length, 2048);
        String head = new String(body, 0, headLength, StandardCharsets.ISO_8859_1).trim();
        boolean looksLikeHtml = head.startsWith("<") && !head.startsWith("<?xml");
        if (looksLikeHtml) {
            Matcher meta = META_CHARSET.matcher(head);
            if (meta.find()) return decodeWith(body, meta.group(1));
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static String decodeWith(byte[] body, String charset) {
        try {
            return new String(body, Charset.forName(charset));
        } catch (Exception e) {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private RequestConfig requestConfig() {
        RequestConfig.Builder builder = RequestConfig.custom()
                .setConnectTimeout(config.timeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setResponseTimeout(config.timeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(config.timeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .setCookieSpec(StandardCookieSpec.RELAXED)
                .setRedirectsEnabled(true)
                .setMaxRedirects(5);
        org.apache.hc.core5.http.HttpHost proxy = proxies.next();
        if (proxy != null) builder.setProxy(proxy);
        return builder.build();
    }

    private void applyStealthHeaders(org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                                     String url) {
        String ua = userAgents.next();
        request.setHeader("User-Agent", ua);
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8");
        request.setHeader("Referer", url);
        fingerprint.headers().forEach(request::setHeader);
    }

    private static void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public CookieJar cookies() { return cookies; }
    public int totalRetries() { return totalRetries.get(); }
    public int captchaChallenges() { return captchaChallenges.get(); }
    public int blockedResponses() { return blockedResponses.get(); }

    /**
     * Registers Basic-auth credentials scoped to the site's host and port. The scope is
     * deliberately narrow so HttpClient never forwards them to a different host.
     */
    public void withBasicAuth(String siteUrl, String username, String password) {
        if (siteUrl == null || username == null || password == null) return;
        try {
            URI uri = URI.create(siteUrl);
            if (!uri.isAbsolute()) {
                log.warn("Refusing to register Basic-auth credentials for a relative URL: {}", siteUrl);
                return;
            }
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null) {
                // URI.getHost() is null for underscores and some IDNs; fall back to authority.
                host = uri.getAuthority();
                if (host != null && host.contains("@")) host = host.substring(host.indexOf('@') + 1);
                if (host != null && port < 0 && host.contains(":")) {
                    int colon = host.lastIndexOf(':');
                    try {
                        port = Integer.parseInt(host.substring(colon + 1));
                        host = host.substring(0, colon);
                    } catch (NumberFormatException ignored) {
                        // Keep the original authority as host.
                    }
                }
            }
            if (host == null || host.isBlank()) {
                log.warn("Could not determine host for Basic-auth credentials: {}", siteUrl);
                return;
            }
            credentials.setCredentials(new AuthScope(host, port),
                    new UsernamePasswordCredentials(username, password.toCharArray()));
        } catch (IllegalArgumentException e) {
            log.warn("Could not scope Basic-auth credentials to {}: {}", siteUrl, e.getMessage());
        }
    }

    /** Shared flag so parallel workers log in only once per run. */
    public boolean isAuthenticated() { return authenticated.get(); }
    public void markAuthenticated() { authenticated.set(true); }

    /** Clears session state before a new job reuses this client. */
    public void resetAuthentication() {
        authenticated.set(false);
        cookies.clear();
    }

    /** Connects transport counters to the run report so retries/captchas/blocks are visible. */
    public void attachMetrics(MetricsCollector metrics) { this.metrics = metrics; }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
