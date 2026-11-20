package com.crawlforge.scraper.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.exception.RetryExhaustedException;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ResilientHttpClientTest {

    private ScraperConfig fastConfig() throws IOException {
        Path file = Files.createTempFile("crawlforge-test", ".properties");
        Files.writeString(file, """
                scraper.retry.attempts=3
                scraper.retry.baseDelayMs=1
                scraper.rate.requestsPerSecond=1000
                scraper.timeoutMs=2000
                """);
        return ScraperConfig.load(file);
    }

    @Test
    void retriesTransientServerErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (LocalServer server = LocalServer.start()) {
            server.route("/flaky", exchange -> {
                int attempt = requests.incrementAndGet();
                if (attempt < 3) LocalServer.respond(exchange, 500, "temporary failure");
                else LocalServer.respond(exchange, 200, "finally ok");
            });
            try (ResilientHttpClient http = new ResilientHttpClient(fastConfig())) {
                HttpResponse response = http.fetch(server.url("/flaky"));
                assertEquals("finally ok", response.body());
                assertEquals(3, requests.get());
                assertTrue(http.totalRetries() >= 2);
            }
        }
    }

    @Test
    void detectsCaptchaChallengePages() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/captcha", exchange ->
                    LocalServer.respond(exchange, 200, "<html>Please solve the reCAPTCHA</html>"));
            try (ResilientHttpClient http = new ResilientHttpClient(fastConfig())) {
                RetryExhaustedException error = assertThrows(RetryExhaustedException.class,
                        () -> http.fetch(server.url("/captcha")));
                assertTrue(error.toString().contains("CAPTCHA")
                        || error.getCause() != null && error.getCause().getMessage().contains("CAPTCHA"));
                assertEquals(3, http.captchaChallenges());
            }
        }
    }

    @Test
    void keepsSessionCookiesAcrossRequests() throws Exception {
        try (LocalServer server = LocalServer.start()) {
            server.route("/session", exchange -> {
                exchange.getResponseHeaders().add("Set-Cookie", "sid=abc123; Path=/");
                LocalServer.respond(exchange, 200, "session ok");
            });
            try (ResilientHttpClient http = new ResilientHttpClient(fastConfig())) {
                http.fetch(server.url("/session"));
                assertEquals(1, http.cookies().size());
                assertTrue(http.cookies().cookieHeaderFor("127.0.0.1").contains("sid=abc123"));
            }
        }
    }
}
