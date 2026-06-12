package cc.fascinated.monitor.metrics.shared.support;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public final class HistogramAccumulator {
    public static final double[] DURATION_BUCKETS = {0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10};
    public static final double[] PAYLOAD_BUCKETS = {1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216};

    private final double[] buckets;
    private final AtomicLong[] bucketCounts;
    private final DoubleAdder sum = new DoubleAdder();
    private final AtomicLong count = new AtomicLong();

    public HistogramAccumulator(double[] buckets) {
        this.buckets = buckets.clone();
        this.bucketCounts = new AtomicLong[buckets.length + 1];
        for (int index = 0; index < this.bucketCounts.length; index++) {
            this.bucketCounts[index] = new AtomicLong();
        }
    }

    public void observe(double value) {
        this.sum.add(value);
        this.count.incrementAndGet();
        int bucketIndex = bucketIndex(value);
        this.bucketCounts[bucketIndex].incrementAndGet();
    }

    public double sum() {
        return this.sum.sum();
    }

    public long count() {
        return this.count.get();
    }

    public double[] buckets() {
        return this.buckets;
    }

    public long bucketCount(int index) {
        return this.bucketCounts[index].get();
    }

    private int bucketIndex(double value) {
        for (int index = 0; index < this.buckets.length; index++) {
            if (value <= this.buckets[index]) {
                return index;
            }
        }
        return this.buckets.length;
    }

    @Override
    public String toString() {
        return "HistogramAccumulator{buckets=" + Arrays.toString(this.buckets)
                + ", count=" + count() + ", sum=" + sum() + "}";
    }
}
