package com.crawlforge.scraper.payment;

import java.time.Instant;

/** PCI-aware transaction record used by the payment/BPO client integration demo. */
public record MerchantTransaction(String transactionId, String cardLast4, String cardBrand,
                                  double amount, String currency, String status, Instant createdAt) {
}
