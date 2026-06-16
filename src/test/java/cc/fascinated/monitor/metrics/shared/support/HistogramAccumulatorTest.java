package cc.fascinated.monitor.metrics.shared.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramAccumulatorTest {
    @Test
    void observesIntoBuckets() {
        HistogramAccumulator histogram = new HistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);
        histogram.observe(0.1);
        histogram.observe(0.5);
        histogram.observe(15);

        assertEquals(3, histogram.count());
        assertEquals(15.6, histogram.sum(), 0.001);
        assertEquals(1, histogram.bucketCount(1));
        assertEquals(1, histogram.bucketCount(3));
        assertEquals(1, histogram.bucketCount(HistogramAccumulator.DURATION_BUCKETS.length));
    }
}
