package com.crawlforge.scraper.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crawlforge.scraper.core.exception.ScraperException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
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
            if (root == null || !root.has("transactions")) {
                throw new ScraperException("PAYLOAD_ERROR", "Merchant payload has no transactions");
            }
            JsonNode items = root.path("transactions");
            if (!items.isArray()) {
                throw new ScraperException("PAYLOAD_ERROR", "Merchant 'transactions' must be an array");
            }
            List<MerchantTransaction> transactions = new ArrayList<>();
            for (JsonNode node : items) {
                String id = node.path("id").asText("");
                JsonNode amountNode = node.get("amount");
                JsonNode createdNode = node.get("created_at");
                JsonNode currencyNode = node.get("currency");
                JsonNode statusNode = node.get("status");
                if (id.isBlank() || amountNode == null || !amountNode.isNumber()
                        || createdNode == null || !createdNode.isIntegralNumber()
                        || statusNode == null || !statusNode.isTextual() || statusNode.asText().isBlank()) {
                    throw new ScraperException("PAYLOAD_ERROR",
                            "Transaction requires id, numeric amount, text status and integral created_at");
                }
                String currency = "BDT";
                if (currencyNode != null && !currencyNode.isNull()) {
                    if (!currencyNode.isTextual() || currencyNode.asText().isBlank()) {
                        throw new ScraperException("PAYLOAD_ERROR", "Invalid currency code");
                    }
                    currency = currencyNode.asText();
                }
                java.math.BigDecimal amount = amountNode.decimalValue();
                double approximate = amount.doubleValue();
                if (amount.signum() <= 0 || !Double.isFinite(approximate)) {
                    throw new ScraperException("PAYLOAD_ERROR", "Amount must be finite and positive");
                }
                Instant createdAt;
                try {
                    createdAt = Instant.ofEpochSecond(createdNode.asLong());
                } catch (java.time.DateTimeException e) {
                    throw new ScraperException("PAYLOAD_ERROR", "created_at is out of range", e);
                }
                String pan = node.path("card_number").asText("");
                if (!CardDataMasker.digits(pan).isEmpty() && !CardDataMasker.valid(pan)) {
                    throw new ScraperException("PAYLOAD_ERROR", "Invalid card number in transaction " + id);
                }
                String last4 = CardDataMasker.digits(pan);
                if (last4.length() > 4) last4 = last4.substring(last4.length() - 4);
                transactions.add(new MerchantTransaction(
                        id, last4, CardDataMasker.brand(pan), amount, currency,
                        statusNode.asText(), createdAt));
            }
            return List.copyOf(transactions);
        } catch (IOException | IllegalArgumentException | java.time.DateTimeException e) {
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
            String supplied = signature == null ? "" : signature.trim().toLowerCase(Locale.ROOT);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) {
                throw new ScraperException("SIGNATURE_ERROR", "Merchant webhook signature mismatch");
            }
        } catch (Exception e) {
            if (e instanceof ScraperException scraperException) throw scraperException;
            throw new ScraperException("SIGNATURE_ERROR", "Could not verify webhook signature", e);
        }
    }
}
