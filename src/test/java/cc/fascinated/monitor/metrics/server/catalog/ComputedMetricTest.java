package cc.fascinated.monitor.metrics.server.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComputedMetricTest {
    @Test
    void diskEtaUsesRegressionSlopeAndDropsFlatOrImplausibleDisks() {
        String used = "monitor_disk_used_bytes{server_id=\"7\"}";
        String total = "monitor_disk_total_bytes{server_id=\"7\"}";
        assertEquals(
                "((" + total + " - " + used + ") / (deriv(" + used + "[1d]) > 0)) < 315360000"
                        + " and (lifetime(" + used + "[1d]) >= 3600)",
                ComputedMetric.DISK_ETA_UNTIL_FULL.promql(7)
        );
    }

    @Test
    void smartWearEtaUsesLifetimeAverageAndPrefersNvmePercentage() {
        String sel = "{server_id=\"7\"}";
        String used = "((monitor_smart_percentage_used" + sel + " or monitor_smart_endurance_used_percent" + sel + ") > 0) < 100";
        assertEquals(
                "((100 - (" + used + ")) / (" + used + ") * monitor_smart_power_on_hours" + sel + " * 3600) < 1576800000",
                ComputedMetric.SMART_ETA_UNTIL_WORN_OUT.promql(7)
        );
    }
}
