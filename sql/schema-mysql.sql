-- MySQL variant (AUTO_INCREMENT / TEXT).
CREATE TABLE IF NOT EXISTS scraped_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    title VARCHAR(1000),
    content TEXT,
    quality_score DOUBLE,
    attributes TEXT,
    scraped_at DATETIME
);
