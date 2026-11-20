package com.crawlforge.scraper.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crawlforge.scraper.core.exception.ConfigurationException;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Reads declarative JSON scrape recipes from config/sites. */
public final class SiteConfigLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public SiteConfig load(Path file) {
        try {
            JsonNode root = mapper.readTree(Files.readAllBytes(file));
            if (root == null || !root.isObject()) {
                throw new ConfigurationException("Site recipe must be a JSON object: " + file);
            }
            SiteConfig.Builder b = SiteConfig.builder()
                    .name(text(root, "name", file.getFileName().toString()))
                    .sourceType(SourceType.valueOf(text(root, "sourceType", "HTML").toUpperCase()))
                    .baseUrl(text(root, "baseUrl", null))
                    .loginUrl(text(root, "loginUrl", null))
                    .username(envOr("SCRAPER_SITE_USERNAME", text(root, "username", null)))
                    .password(envOr("SCRAPER_SITE_PASSWORD", text(root, "password", null)))
                    .itemSelector(text(root, "itemSelector", null))
                    .nextPageSelector(text(root, "nextPageSelector", null))
                    .waitForSelector(text(root, "waitForSelector", null))
                    .jsonResultPath(text(root, "jsonResultPath", "$"))
                    .jsonIdField(text(root, "jsonIdField", "id"))
                    .maxPages(intValue(root, "maxPages", 50))
                    .maxDepth(intValue(root, "maxDepth", 3))
                    .followLinks(bool(root, "followLinks", true))
                    .javascriptRequired(bool(root, "javascriptRequired", false))
                    .csvHasHeader(bool(root, "csvHasHeader", true));

            JsonNode seeds = root.get("seedUrls");
            if (seeds != null && seeds.isArray()) seeds.forEach(s -> b.seedUrl(s.asText()));

            addMap(root.get("fields"), b::field);
            addMap(root.get("xpathFields"), b::xpathField);
            return b.build();
        } catch (IOException | IllegalArgumentException e) {
            throw new ConfigurationException("Invalid site config: " + file, e);
        }
    }

    public Map<String, SiteConfig> loadAll(Path directory) {
        java.util.Map<String, SiteConfig> configs = new java.util.LinkedHashMap<>();
        if (!Files.isDirectory(directory)) return configs;
        try (var stream = Files.list(directory)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            for (Path file : files) {
                SiteConfig config = load(file);
                configs.put(config.name(), config);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Could not read site configs in " + directory, e);
        }
        return configs;
    }

    private void addMap(JsonNode node, java.util.function.BiConsumer<String, String> sink) {
        if (node == null) return;
        if (!node.isObject()) throw new ConfigurationException("Field mappings must be JSON objects");
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            sink.accept(e.getKey(), e.getValue().asText());
        }
    }

    private static String envOr(String variable, String fallback) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String text(JsonNode n, String key, String def) {
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? def : v.asText();
    }
    private static int intValue(JsonNode n, String key, int def) {
        JsonNode v = n.get(key);
        return v == null ? def : v.asInt();
    }
    private static boolean bool(JsonNode n, String key, boolean def) {
        JsonNode v = n.get(key);
        return v == null ? def : v.asBoolean();
    }
}
