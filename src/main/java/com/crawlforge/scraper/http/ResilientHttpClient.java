package com.crawlforge.scraper.http;

import com.crawlforge.scraper.core.config.RetryPolicy;
import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.exception.AntiBotException;
import com.crawlforge.scraper.core.exception.CaptchaDetectedException;
import com.crawlforge.scraper.core.exception.FetchException;
import com.crawlforge.scraper.core.exception.RateLimitException;
import com.crawlforge.scraper.core.exception.RetryExhaustedException;
import com.crawlforge.scraper.stealth.AntiBotDetector;
import com.crawlforge.scraper.stealth.BrowserFingerprint;
import com.crawlforge.scraper.stealth.CaptchaSolver;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP transport with connection pooling, rate limiting, proxy/UA rotation, cookie sessions,
 * typed exceptions, retries with backoff and anti-bot detection. This is the single place where
 * raw network I/O happens.
 */
public final class ResilientHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ResilientHttpClient.class);

    private final ScraperConfig config;
    private final CloseableHttpClient client;
    private final CookieJar cookies = new CookieJar();
    private final RateLimiter rateLimiter;
    private final RetryPolicy retryPolicy;
    private final UserAgentRotator userAgents;
    private final ProxyRotator proxies;
    private final AntiBotDetector antiBotDetector = new AntiBotDetector();
    private final CaptchaSolver captchaSolver;
    private final BrowserFingerprint fingerprint;

    private final AtomicInteger totalRetries = new AtomicInteger();
    private final AtomicInteger captchaChallenges = new AtomicInteger();
    private final AtomicInteger blockedResponses = new AtomicInteger();

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
        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
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
            } catch (CaptchaDetectedException e) {
                // A solver returning false means "unrecoverable": fail fast, do not re-challenge.
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FetchException("Fetch interrupted for " + url, e);
            } catch (Exception e) {
                last = e;
                if (!retryPolicy.shouldRetry(attempt)) break;
                totalRetries.incrementAndGet();
                log.warn("Attempt {}/{} failed for {}: {} - retrying in {}", attempt,
                        retryPolicy.maxAttempts(), url, e.getMessage(), retryPolicy.delayFor(attempt));
                sleep(retryPolicy.delayFor(attempt).toMillis());
            }
        }
        throw new RetryExhaustedException("Fetch failed after " + retryPolicy.maxAttempts()
                + " attempts: " + url, last);
    }

    /** GET a URL as text with retries, backoff and full anti-bot handling. */
    public HttpResponse fetch(String url) {
        return withRetries(url, request -> validate(execute(request, url)));
    }

    /** GET a URL as raw bytes; never decodes binary formats such as PDF. */
    public BinaryResponse fetchBytes(String url) {
        return withRetries(url, request -> new BinaryResponse(
                client.execute(request, response -> {
                    int status = response.getCode();
                    if (status >= 400) throw new FetchException("HTTP " + status + " from " + url);
                    return response.getEntity() == null
                            ? new byte[0] : response.getEntity().getContent().readAllBytes();
                })));
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
            return validate(execute(post, url));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("POST interrupted for " + url, e);
        } catch (java.io.IOException e) {
            throw new FetchException("POST failed for " + url, e);
        }
    }

    private HttpResponse execute(org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                                 String url) throws IOException {
        return client.execute(request, response -> {
            int status = response.getCode();
            String body = response.getEntity() == null ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            Map<String, String> headers = new LinkedHashMap<>();
            for (Header header : response.getHeaders()) headers.put(header.getName(), header.getValue());
            return new HttpResponse(status, body, url, Map.copyOf(headers));
        });
    }

    private HttpResponse validate(HttpResponse response) {
        if (response.status() == 429) {
            blockedResponses.incrementAndGet();
            throw new RateLimitException("HTTP 429 from " + response.url());
        }
        if (response.status() >= 500) {
            throw new FetchException("HTTP " + response.status() + " from " + response.url());
        }
        if (response.status() >= 400) {
            throw new FetchException("HTTP " + response.status() + " from " + response.url());
        }
        AntiBotDetector.ThreatLevel threat = antiBotDetector.detect(response.body(), response.status());
        if (threat == AntiBotDetector.ThreatLevel.CAPTCHA) {
            captchaChallenges.incrementAndGet();
            if (captchaSolver.solve(response.url(), response.body())) {
                throw new FetchException("CAPTCHA solved, retrying " + response.url());
            }
            throw new CaptchaDetectedException("CAPTCHA challenge at " + response.url());
        }
        if (threat == AntiBotDetector.ThreatLevel.BLOCKED) {
            blockedResponses.incrementAndGet();
            throw new AntiBotException("Bot challenge from " + response.url());
        }
        return response;
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

    @Override
    public void close() throws IOException {
        client.close();
    }
}
