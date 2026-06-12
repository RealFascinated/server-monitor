package cc.fascinated.monitor.metrics.vm.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromqlQueryBuilderTest {
    @Test
    void ratePerMinute() {
        assertEquals(
                "rate(monitor_platform_ingests_total[1m]) * 60",
                PromqlQueryBuilder.metric("monitor_platform_ingests_total").rate("1m").multiply(60).build()
        );
    }

    @Test
    void increaseOverDay() {
        assertEquals(
                "increase(monitor_platform_ingests_total[24h])",
                PromqlQueryBuilder.metric("monitor_platform_ingests_total").increase("24h").build()
        );
    }

    @Test
    void histogramQuantile() {
        assertEquals(
                "histogram_quantile(0.95, sum(rate(monitor_platform_ingest_duration_seconds_bucket[5m])) by (le))",
                PromqlQueryBuilder.metric("monitor_platform_ingest_duration_seconds").histogramQuantile(0.95, "5m").build()
        );
    }
}
