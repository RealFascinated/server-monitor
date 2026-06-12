package cc.fascinated.monitor.metrics.shared.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LabeledCounterAccumulator {
    private final ConcurrentHashMap<String, AtomicLong> values = new ConcurrentHashMap<>();

    public void increment(String labelKey) {
        this.values.computeIfAbsent(labelKey, ignored -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Long> snapshot() {
        ConcurrentHashMap<String, Long> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : this.values.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return snapshot;
    }
}
