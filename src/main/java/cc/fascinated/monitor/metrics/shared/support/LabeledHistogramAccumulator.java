package cc.fascinated.monitor.metrics.shared.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LabeledHistogramAccumulator {
    private final double[] buckets;
    private final ConcurrentHashMap<String, HistogramAccumulator> histograms = new ConcurrentHashMap<>();

    public LabeledHistogramAccumulator(double[] buckets) {
        this.buckets = buckets;
    }

    public void observe(String labelKey, double value) {
        this.histograms.computeIfAbsent(labelKey, ignored -> new HistogramAccumulator(this.buckets)).observe(value);
    }

    public Map<String, HistogramAccumulator> snapshot() {
        return Map.copyOf(this.histograms);
    }
}
