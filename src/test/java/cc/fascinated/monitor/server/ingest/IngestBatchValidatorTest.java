package cc.fascinated.monitor.server.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestBatchRequest;
import cc.fascinated.monitor.server.ingest.IngestBatchValidator.BatchPlan;
import cc.fascinated.monitor.support.TestFixtures;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestBatchValidatorTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final long NOW_S = NOW.getEpochSecond();

    private final MonitorIngestProperties properties = new MonitorIngestProperties();
    private final IngestBatchValidator validator =
            new IngestBatchValidator(this.properties, new IngestSampleChecker(this.properties));
    private final JsonMapper mapper = JsonMapper.builder().build();

    private IngestBatchRequest batch(Long sentAt, long... timestamps) {
        return this.mapper.readValue(TestFixtures.batchPayload(sentAt, timestamps), IngestBatchRequest.class);
    }

    @Test
    void acceptsSamplesInsideWindowOldestFirst() {
        BatchPlan plan = this.validator.plan(batch(NOW_S, NOW_S - 10, NOW_S - 300, NOW_S - 100), NOW);

        assertEquals(3, plan.accepted().size());
        assertEquals(NOW_S - 300, plan.accepted().get(0).timestamp().getEpochSecond());
        assertEquals(NOW_S - 10, plan.accepted().get(2).timestamp().getEpochSecond());
        assertEquals(Map.of(), plan.skipped());
    }

    @Test
    void skipsSamplesOlderThanBackfillWindow() {
        long tooOld = NOW_S - 6 * 3600 - 5;
        BatchPlan plan = this.validator.plan(batch(NOW_S, tooOld, NOW_S - 5 * 3600), NOW);

        assertEquals(1, plan.accepted().size());
        assertEquals(Map.of("too_old", 1), plan.skipped());
    }

    @Test
    void skipsSamplesFromTheFuture() {
        BatchPlan plan = this.validator.plan(batch(NOW_S, NOW_S + 3600, NOW_S + 30), NOW);

        assertEquals(1, plan.accepted().size());
        assertEquals(Map.of("too_new", 1), plan.skipped());
    }

    @Test
    void correctsForAgentClockSkew() {
        // Agent clock is 2h behind ours: sentAt and every sample shift by the same amount.
        long skew = 7200;
        BatchPlan plan = this.validator.plan(batch(NOW_S - skew, NOW_S - skew - 600), NOW);

        assertEquals(1, plan.accepted().size());
        assertEquals(NOW_S - 600, plan.accepted().get(0).timestamp().getEpochSecond());
    }

    @Test
    void agentClockAheadCannotPushSamplesIntoTheFuture() {
        long skew = -3600; // agent is an hour ahead
        BatchPlan plan = this.validator.plan(batch(NOW_S - skew, NOW_S - skew - 30), NOW);

        assertEquals(NOW_S - 30, plan.accepted().get(0).timestamp().getEpochSecond());
    }

    @Test
    void rejectsBatchWhenClockOffsetIsImplausible() {
        assertThrows(BadRequestException.class,
                () -> this.validator.plan(batch(NOW_S - 3 * 86400, NOW_S - 3 * 86400 - 10), NOW));
    }

    @Test
    void skipsDuplicateTimestamps() {
        BatchPlan plan = this.validator.plan(batch(NOW_S, NOW_S - 60, NOW_S - 60, NOW_S - 61), NOW);

        assertEquals(2, plan.accepted().size());
        assertEquals(Map.of("duplicate", 1), plan.skipped());
    }

    @Test
    void skipsSamplesWithoutTimestamp() {
        IngestBatchRequest request = this.mapper.readValue(
                "{\"sentAt\": " + NOW_S + ", \"samples\": [" + TestFixtures.minimalIngestPayload() + "]}",
                IngestBatchRequest.class);

        BatchPlan plan = this.validator.plan(request, NOW);

        assertEquals(0, plan.accepted().size());
        assertEquals(Map.of("invalid", 1), plan.skipped());
    }

    @Test
    void rejectsEmptyOversizedAndUnstampedBatches() {
        assertThrows(BadRequestException.class, () -> this.validator.plan(batch(NOW_S), NOW));
        assertThrows(BadRequestException.class, () -> this.validator.plan(null, NOW));
        assertThrows(BadRequestException.class, () -> this.validator.plan(batch(null, NOW_S - 10), NOW));

        long[] many = new long[this.properties.getMaxBatchSamples() + 1];
        for (int i = 0; i < many.length; i++) {
            many[i] = NOW_S - 1 - i;
        }
        assertThrows(BadRequestException.class, () -> this.validator.plan(batch(NOW_S, many), NOW));
    }
}
