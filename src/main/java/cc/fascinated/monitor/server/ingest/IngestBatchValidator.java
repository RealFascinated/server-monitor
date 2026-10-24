package cc.fascinated.monitor.server.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestBatchRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which samples of a backfill batch are believable. Nothing the agent says about time or
 * content is trusted: timestamps are corrected for clock skew and then bounded to a window around
 * the server's own clock.
 */
@Component
public class IngestBatchValidator {
    public static final String REASON_TOO_OLD = "too_old";
    public static final String REASON_TOO_NEW = "too_new";
    public static final String REASON_DUPLICATE = "duplicate";

    private final MonitorIngestProperties properties;
    private final IngestSampleChecker sampleChecker;

    public IngestBatchValidator(MonitorIngestProperties properties, IngestSampleChecker sampleChecker) {
        this.properties = properties;
        this.sampleChecker = sampleChecker;
    }

    public record AcceptedSample(Instant timestamp, IngestServerMetrics metrics) {}

    /**
     * @param accepted samples to write, oldest first
     * @param skipped  number of samples dropped, by reason
     */
    public record BatchPlan(List<AcceptedSample> accepted, Map<String, Integer> skipped) {}

    /**
     * @throws BadRequestException when the batch as a whole cannot be trusted
     */
    public BatchPlan plan(IngestBatchRequest batch, Instant now) {
        List<IngestServerMetrics> samples = batch == null ? null : batch.samples();
        if (samples == null || samples.isEmpty()) {
            throw new BadRequestException("batch must contain at least one sample");
        }
        if (samples.size() > this.properties.getMaxBatchSamples()) {
            throw new BadRequestException("batch contains more than %d samples".formatted(this.properties.getMaxBatchSamples()));
        }
        if (batch.sentAt() == null || batch.sentAt() <= 0) {
            throw new BadRequestException("batch sentAt is required");
        }

        // How far the agent's clock is behind (+) or ahead (-) of ours; applied to every sample.
        long offsetSeconds = now.getEpochSecond() - batch.sentAt();
        if (Math.abs(offsetSeconds) > this.properties.getMaxClockOffset().toSeconds()) {
            throw new BadRequestException("agent clock differs from the server clock by too much to backfill");
        }

        Instant oldest = now.minus(this.properties.getMaxBackfill());
        Instant newest = now.plus(this.properties.getMaxFuture());

        Map<String, Integer> skipped = new LinkedHashMap<>();
        Set<Long> seen = new HashSet<>();
        List<AcceptedSample> accepted = new ArrayList<>(samples.size());
        for (IngestServerMetrics sample : samples) {
            String reason = sample == null || sample.timestamp() == null || sample.timestamp() <= 0
                    ? IngestSampleChecker.REASON_INVALID
                    : this.sampleChecker.rejectReason(sample);
            if (reason == null) {
                Instant timestamp = Instant.ofEpochSecond(sample.timestamp() + offsetSeconds);
                if (timestamp.isBefore(oldest)) {
                    reason = REASON_TOO_OLD;
                } else if (timestamp.isAfter(newest)) {
                    reason = REASON_TOO_NEW;
                } else if (!seen.add(timestamp.getEpochSecond())) {
                    reason = REASON_DUPLICATE;
                } else {
                    accepted.add(new AcceptedSample(timestamp, sample));
                    continue;
                }
            }
            skipped.merge(reason, 1, Integer::sum);
        }

        accepted.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
        return new BatchPlan(accepted, skipped);
    }

    /** Exposed for tests and callers that need the configured window. */
    public Duration maxBackfill() {
        return this.properties.getMaxBackfill();
    }
}
