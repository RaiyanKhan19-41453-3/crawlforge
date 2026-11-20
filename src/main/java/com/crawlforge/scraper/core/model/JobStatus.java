package com.crawlforge.scraper.core.model;

/** Lifecycle of a crawl job, mirroring a typical JIRA workflow. */
public enum JobStatus {
    CREATED,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    PARTIAL,
    FAILED
}
