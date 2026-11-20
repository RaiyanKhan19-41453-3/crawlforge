package com.crawlforge.scraper.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crawlforge.scraper.core.exception.ScraperException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class PaymentwallPayloadParserTest {

    private static final String SECRET = "webhook-secret";
    private static final String PAYLOAD = """
            {"transactions":[{"id":"tx-1","card_number":"4111111111111111",
            "amount":10.5,"currency":"BDT","status":"approved","created_at":1700000000}]}
            """;

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesSignedPayloadAndMasksCard() throws Exception {
        List<MerchantTransaction> transactions =
                new PaymentwallPayloadParser().parse(PAYLOAD, sign(PAYLOAD), SECRET);
        assertEquals(1, transactions.size());
        assertEquals("tx-1", transactions.get(0).transactionId());
        assertEquals("1111", transactions.get(0).cardLast4());
        assertEquals("VISA", transactions.get(0).cardBrand());
    }

    @Test
    void rejectsBadSignature() throws Exception {
        assertThrows(ScraperException.class,
                () -> new PaymentwallPayloadParser().parse(PAYLOAD, "deadbeef", SECRET));
    }

    @Test
    void rejectsInvalidCardNumber() throws Exception {
        String payload = "{\"transactions\":[{\"id\":\"tx-2\","
                + "\"card_number\":\"4111111111111112\",\"amount\":1}]}";
        ScraperException error = assertThrows(ScraperException.class,
                () -> new PaymentwallPayloadParser().parse(payload, sign(payload), SECRET));
        assertEquals("PAYLOAD_ERROR", error.errorCode());
    }
}
