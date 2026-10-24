package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.ingest")
public class MonitorIngestProperties {
    /** Maximum size of an ingest request body after any Content-Encoding has been removed. */
    private DataSize maxRequestSize = DataSize.ofMegabytes(16);

    /** Backfilled samples older than this are skipped. */
    private Duration maxBackfill = Duration.ofHours(6);

    /** Samples further ahead of the server clock than this (after skew correction) are skipped. */
    private Duration maxFuture = Duration.ofSeconds(60);

    /** Batches whose sender clock differs from ours by more than this are rejected outright. */
    private Duration maxClockOffset = Duration.ofHours(24);

    /** Maximum number of samples in one batch request. */
    private int maxBatchSamples = 100;

    /** Maximum entries in any one list of a sample (disks, containers, cores, ...). */
    private int maxListItems = 2048;

    /** Maximum length of names that become metric label values. */
    private int maxLabelLength = 256;

    /** Per-server cap on backfilled samples accepted per minute. */
    private int maxBackfilledSamplesPerMinute = 1200;
}
