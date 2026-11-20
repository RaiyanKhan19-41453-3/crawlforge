package com.crawlforge.scraper.parsers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses JSON APIs and embedded JSON payloads using simple dotted paths with array indices. */
public final class JsonParser implements Parser {

    // Preserve the declared precision of numeric fields such as prices.
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    @Override
    public boolean supports(SourceType type) { return type == SourceType.JSON; }

    @Override
    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        try {
            JsonNode root = mapper.readTree(DataNormalizer.stripBom(raw));
            if (root == null || root.isMissingNode()) return List.of();
            JsonNode collection = resolvePath(root, site.jsonResultPath());
            List<JsonNode> nodes = new ArrayList<>();
            if (collection != null && collection.isArray()) {
                collection.forEach(nodes::add);
            } else if (collection != null && !collection.isMissingNode()) {
                nodes.add(collection);
            }
            List<ScrapedRecord> results = new ArrayList<>();
            for (JsonNode node : nodes) {
                Map<String, String> attributes = new LinkedHashMap<>();
                if (site.fields().isEmpty()) {
                    Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> field = it.next();
                        attributes.put(field.getKey(), text(field.getValue()));
                    }
                } else {
                    site.fields().forEach((name, pointer) ->
                            attributes.put(name, text(node.at(pointer.startsWith("/") ? pointer : "/" + pointer))));
                }
                attributes.replaceAll((key, value) -> DataNormalizer.normalize(value));
                // jsonIdField is the client's identifier column when fields do not map it.
                if (!attributes.containsKey("id") && site.jsonIdField() != null) {
                    String id = text(node.at("/" + site.jsonIdField()));
                    if (!id.isBlank()) attributes.put("id", id);
                }

                String url = text(node.at("/url"));
                if (url.isBlank()) url = site.baseUrl();
                String title = attributes.getOrDefault("title", text(node.at("/title")));
                String content = attributes.getOrDefault("content", node.toString());
                results.add(ScrapedRecord.builder()
                        .source(site.name())
                        .url(url)
                        .title(title)
                        .content(DataNormalizer.truncate(content, 10_000))
                        .attributes(attributes)
                        .qualityScore(DataValidator.score(url, content, attributes))
                        .build());
            }
            return List.copyOf(results);
        } catch (IOException e) {
            throw new ParseException("Could not parse JSON for site " + site.name(), e);
        }
    }

    private static JsonNode resolvePath(JsonNode root, String path) {
        if (path == null || path.isBlank() || "$".equals(path)) return root;
        String clean = path.replaceFirst("^\\$", "").replaceFirst("^\\.", "");
        clean = clean.replaceAll("\\[(\\d+)\\]", "/$1").replace('.', '/');
        return root.at("/" + clean);
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        if (!node.isValueNode()) return node.toString();
        if (node.isNumber()) {
            try {
                return node.decimalValue().toPlainString();
            } catch (NumberFormatException ignored) {
                return node.asText();
            }
        }
        return node.asText();
    }
}
