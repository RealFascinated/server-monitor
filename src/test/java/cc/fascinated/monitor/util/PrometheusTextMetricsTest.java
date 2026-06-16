package cc.fascinated.monitor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusTextMetricsTest {

    @Test
    void sumsMatchingGaugeValues() {
        String body = """
                # HELP vm_data_size_bytes Total size of data stored on disk
                vm_data_size_bytes{type="storage/small"} 1000
                vm_data_size_bytes{type="storage/big"} 2000
                vm_data_size_bytes{type="indexdb/file"} 500
                vm_free_disk_space_bytes{path="/storage"} 999
                """;

        assertEquals(3500L, PrometheusTextMetrics.sumGaugeValues(body, "vm_data_size_bytes").orElseThrow());
    }

    @Test
    void ignoresUnrelatedMetricsWithSharedPrefix() {
        String body = "vm_data_size_bytes_total 42\n";

        assertTrue(PrometheusTextMetrics.sumGaugeValues(body, "vm_data_size_bytes").isEmpty());
    }

    @Test
    void returnsEmptyWhenMetricMissing() {
        assertTrue(PrometheusTextMetrics.sumGaugeValues("vm_up 1\n", "vm_data_size_bytes").isEmpty());
    }
}
