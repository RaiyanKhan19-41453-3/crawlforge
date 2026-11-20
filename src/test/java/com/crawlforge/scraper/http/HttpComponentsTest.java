package com.crawlforge.scraper.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.junit.jupiter.api.Test;

class HttpComponentsTest {

    @Test
    void proxyRotatorParsesAndRotates() {
        ProxyRotator rotator = new ProxyRotator(List.of("proxy1.test:8080", "user:pass@proxy2.test:8081"));
        assertEquals(2, rotator.size());
        assertTrue(rotator.hasAny());
        assertEquals("proxy1.test", rotator.next().getHostName());
        assertEquals("proxy2.test", rotator.next().getHostName());
        assertEquals("proxy1.test", rotator.next().getHostName());
    }

    @Test
    void userAgentRotatorCyclesWithoutRepeatingAdjacentValues() {
        UserAgentRotator rotator = new UserAgentRotator(List.of("UA-1", "UA-2"));
        assertEquals(2, rotator.size());
        assertEquals("UA-1", rotator.next());
        assertEquals("UA-2", rotator.next());
        assertEquals("UA-1", rotator.next());
    }

    @Test
    void cookieHeaderDoesNotLeakToSimilarHostnames() {
        CookieJar jar = new CookieJar();
        BasicClientCookie cookie = new BasicClientCookie("sid", "abc123");
        cookie.setDomain("example.com");
        jar.add(cookie);

        assertTrue(jar.cookieHeaderFor("example.com").contains("sid=abc123"));
        assertTrue(jar.cookieHeaderFor("shop.example.com").contains("sid=abc123"));
        assertFalse(jar.cookieHeaderFor("notexample.com").contains("sid=abc123"));
    }
}
