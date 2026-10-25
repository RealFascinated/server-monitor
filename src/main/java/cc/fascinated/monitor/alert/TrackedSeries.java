package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.SeriesState;

import java.util.Map;

/** In-memory alert state for one (rule, server, series). */
final class TrackedSeries {
    final SeriesState state;
    final int windowSeconds;
    final String metric;
    final Map<String, String> labels;
    volatile long lastNotifiedMillis;

    TrackedSeries(int windowSeconds, String metric, Map<String, String> labels) {
        this.state = new SeriesState(windowSeconds);
        this.windowSeconds = windowSeconds;
        this.metric = metric;
        this.labels = labels;
    }
}
