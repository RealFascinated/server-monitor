package cc.fascinated.monitor.server.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import cc.fascinated.monitor.exception.impl.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a misbehaving agent from amplifying writes: one backfill batch in flight per server and a
 * cap on backfilled samples per minute.
 */
@Component
public class BackfillLimiter {
    private static final long WINDOW_MILLIS = 60_000;

    private final MonitorIngestProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<Long, ServerWindow> windows = new ConcurrentHashMap<>();

    @Autowired
    public BackfillLimiter(MonitorIngestProperties properties) {
        this(properties, Clock.systemUTC());
    }

    BackfillLimiter(MonitorIngestProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Reserves capacity for {@code samples}; callers must {@link #release} afterwards.
     *
     * @throws TooManyRequestsException when a batch is already running or the per-minute cap is reached
     */
    public void acquire(long serverId, int samples) {
        ServerWindow window = this.windows.computeIfAbsent(serverId, ignored -> new ServerWindow());
        synchronized (window) {
            long now = this.clock.millis();
            if (now - window.startedAt >= WINDOW_MILLIS) {
                window.startedAt = now;
                window.count = 0;
            }
            if (window.inFlight) {
                throw new TooManyRequestsException("A backfill batch is already being processed for this server");
            }
            if (window.count + samples > this.properties.getMaxBackfilledSamplesPerMinute()) {
                throw new TooManyRequestsException("Backfill rate limit reached, retry shortly");
            }
            window.inFlight = true;
            window.count += samples;
        }
    }

    public void release(long serverId) {
        ServerWindow window = this.windows.get(serverId);
        if (window != null) {
            synchronized (window) {
                window.inFlight = false;
            }
        }
    }

    private static final class ServerWindow {
        private boolean inFlight;
        private long startedAt;
        private int count;
    }
}
