package cc.fascinated.monitor.metrics.vm.catalog;

import cc.fascinated.monitor.metrics.vm.series.impl.DiskSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.HostSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TemperatureSeries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VmMetricCatalogTest {
    @Test
    void familyOf_resolvesHostMetrics() {
        assertEquals(MetricFamily.HOST, VmMetricCatalog.familyOf(HostSeries.CPU_USAGE.metricName()));
    }

    @Test
    void familyOf_resolvesSharedHostPrefixMetricsSeparately() {
        assertEquals(MetricFamily.CPU_CORE, VmMetricCatalog.familyOf("monitor_host_cpu_core_pct"));
        assertEquals(MetricFamily.TEMPERATURE, VmMetricCatalog.familyOf(TemperatureSeries.CELSIUS.metricName()));
    }

    @Test
    void fieldName_stripsPrefixAndCamelCases() {
        assertEquals("cpuUsage", VmMetricCatalog.fieldName(HostSeries.CPU_USAGE));
        assertEquals("usedBytes", VmMetricCatalog.fieldName(DiskSeries.USED_BYTES));
    }

    @Test
    void selectorForServer_includesServerIdAndAllGaugeNames() {
        String selector = VmMetricCatalog.selectorForServer(42L);
        assertTrue(selector.contains("server_id=\"42\""));
        assertTrue(selector.contains(HostSeries.CPU_USAGE.metricName()));
        assertTrue(selector.contains(DiskSeries.USED_BYTES.metricName()));
    }

    @Test
    void selectorForServers_buildsMultiServerSelector() {
        String selector = VmMetricCatalog.selectorForServers(
                List.of(1L, 2L),
                List.of(HostSeries.CPU_USAGE, HostSeries.MEM_USAGE)
        );
        assertTrue(selector.contains("server_id=~\"1|2\""), selector);
        assertTrue(selector.contains("monitor_host_cpu_usage"));
        assertTrue(selector.contains("monitor_host_mem_usage"));
    }

    @Test
    void allGaugeNames_isNonEmpty() {
        assertNotNull(VmMetricCatalog.allGaugeNames());
        assertTrue(VmMetricCatalog.allGaugeNames().size() > 50);
    }
}
