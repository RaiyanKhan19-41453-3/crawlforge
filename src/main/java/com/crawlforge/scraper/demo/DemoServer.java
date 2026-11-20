package com.crawlforge.scraper.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Built-in fixture website so the whole crawler can be demonstrated offline, without depending
 * on a third-party site (which could change or block requests at any moment).
 */
public final class DemoServer implements AutoCloseable {

    private final HttpServer server;

    private DemoServer(HttpServer server) { this.server = server; }

    public static DemoServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/page/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String html = path.endsWith("2") ? pageTwo() : pageOne();
            respond(exchange, html, "text/html; charset=utf-8");
        });
        server.createContext("/api/products", exchange -> respond(exchange,
                "{\"products\":[{\"id\":\"SKU-1001\",\"title\":\"Widget A\",\"price\":\"1250.00\"},"
                        + "{\"id\":\"SKU-1002\",\"title\":\"Widget B\",\"price\":\"899.00\"}]}",
                "application/json"));
        server.createContext("/catalog.xml", exchange -> respond(exchange,
                "<catalog><item><title>Widget A</title><price>1250</price></item>"
                        + "<item><title>Widget B</title><price>899</price></item></catalog>",
                "application/xml"));
        server.createContext("/data.csv", exchange -> respond(exchange,
                "id,title,price\nSKU-1001,Widget A,1250\nSKU-1002,Widget B,899\n",
                "text/csv"));
        server.start();
        return new DemoServer(server);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String pageOne() {
        return """
                <html><head><title>Demo Shop</title></head><body>
                  <div class="product"><h2 class="title">Widget A</h2>
                    <span class="price">BDT 1,250.00</span><span id="sku">SKU-1001</span></div>
                  <div class="product"><h2 class="title">Widget B</h2>
                    <span class="price">BDT 899.00</span><span id="sku">SKU-1002</span></div>
                  <div class="product"><h2 class="title">Widget C</h2>
                    <span class="price">BDT 450.00</span><span id="sku">SKU-1003</span></div>
                  <a class="next" href="/page/2">Next page</a>
                </body></html>
                """;
    }

    private static String pageTwo() {
        return """
                <html><head><title>Demo Shop - Page 2</title></head><body>
                  <div class="product"><h2 class="title">Widget D</h2>
                    <span class="price">BDT 2,100.00</span><span id="sku">SKU-1004</span></div>
                  <div class="product"><h2 class="title">Widget E</h2>
                    <span class="price">BDT 75.00</span><span id="sku">SKU-1005</span></div>
                </body></html>
                """;
    }

    @Override
    public void close() { server.stop(0); }
}
