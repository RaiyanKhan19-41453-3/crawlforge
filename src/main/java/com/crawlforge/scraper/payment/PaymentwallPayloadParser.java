package com.crawlforge.scraper.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crawlforge.scraper.core.exception.ScraperException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Demonstrates integration with a merchant/payment provider (Paymentwall-style webhook):
 * HMAC signature verification, parsing and PCI-safe card handling.
 */
public final class PaymentwallPayloadParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<MerchantTransaction> parse(String payload, String signature, String secret) {
        verifySignature(payload, signature, secret);
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode items = root.path("transactions");
            List<MerchantTransaction> transactions = new ArrayList<>();
            for (JsonNode node : items) {
                String pan = node.path("card_number").asText("");
                if (!CardDataMasker.digits(pan).isEmpty() && !CardDataMasker.valid(pan)) {
                    throw new ScraperException("PAYLOAD_ERROR", "Invalid card number in transaction "
                            + node.path("id").asText(""));
                }
                String last4 = CardDataMasker.digits(pan);
                if (last4.length() > 4) last4 = last4.substring(last4.length() - 4);
                transactions.add(new MerchantTransaction(
                        node.path("id").asText(""),
                        last4,
                        CardDataMasker.brand(pan),
                        node.path("amount").asDouble(),
                        node.path("currency").asText("BDT"),
                        node.path("status").asText(""),
                        Instant.ofEpochSecond(node.path("created_at").asLong())));
            }
            return List.copyOf(transactions);
        } catch (IOException | IllegalArgumentException e) {
            throw new ScraperException("PAYLOAD_ERROR", "Invalid merchant payload", e);
        }
    }

    private void verifySignature(String payload, String signature, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new ScraperException("SIGNATURE_ERROR", "Merchant webhook secret is not configured");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!expected.equalsIgnoreCase(signature == null ? "" : signature)) {
                throw new ScraperException("SIGNATURE_ERROR", "Merchant webhook signature mismatch");
            }
        } catch (Exception e) {
            if (e instanceof ScraperException scraperException) throw scraperException;
            throw new ScraperException("SIGNATURE_ERROR", "Could not verify webhook signature", e);
        }
    }
}
