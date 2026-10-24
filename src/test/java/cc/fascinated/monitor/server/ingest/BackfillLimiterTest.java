package cc.fascinated.monitor.server.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import cc.fascinated.monitor.exception.impl.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackfillLimiterTest {
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return this.now; }
    }

    private final MonitorIngestProperties properties = new MonitorIngestProperties();
    private final MutableClock clock = new MutableClock();
    private final BackfillLimiter limiter = new BackfillLimiter(this.properties, this.clock);

    @Test
    void allowsOnlyOneBatchInFlightPerServer() {
        this.limiter.acquire(1, 10);

        assertThrows(TooManyRequestsException.class, () -> this.limiter.acquire(1, 10));
        assertDoesNotThrow(() -> this.limiter.acquire(2, 10));

        this.limiter.release(1);
        assertDoesNotThrow(() -> this.limiter.acquire(1, 10));
    }

    @Test
    void capsBackfilledSamplesPerMinuteAndResetsAfterTheWindow() {
        this.properties.setMaxBackfilledSamplesPerMinute(150);
        this.limiter.acquire(1, 100);
        this.limiter.release(1);

        assertThrows(TooManyRequestsException.class, () -> this.limiter.acquire(1, 100));

        this.clock.advance(Duration.ofSeconds(61));
        assertDoesNotThrow(() -> this.limiter.acquire(1, 100));
    }
}
