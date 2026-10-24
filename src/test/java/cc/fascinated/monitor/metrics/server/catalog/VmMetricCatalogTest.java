package cc.fascinated.monitor.metrics.server.catalog;

import cc.fascinated.monitor.metrics.server.series.AgentSeries;
import cc.fascinated.monitor.metrics.server.series.DockerSeries;
import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import cc.fascinated.monitor.metrics.server.series.SmartSeries;
import cc.fascinated.monitor.metrics.server.series.TemperatureSeries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void agentSeries_areNotServedPerServer() {
        assertNull(VmMetricCatalog.familyOf(AgentSeries.RSS_BYTES.metricName()));
        assertFalse(VmMetricCatalog.allGaugeNames().contains(AgentSeries.RSS_BYTES.metricName()));
    }

    @Test
    void clockSeries_mapToHostSectionFields() {
        assertEquals(MetricFamily.HOST, VmMetricCatalog.familyOf(HostSeries.CLOCK_SYNCED.metricName()));
        assertEquals("clockSynced", VmMetricCatalog.fieldName(HostSeries.CLOCK_SYNCED));
        assertEquals("clockOffsetMs", VmMetricCatalog.fieldName(HostSeries.CLOCK_OFFSET_MS));
    }

    @Test
    void smartSeries_mapToSmartDevicesSection() {
        assertEquals(MetricFamily.SMART, VmMetricCatalog.familyOf(SmartSeries.PERCENTAGE_USED.metricName()));
        assertEquals("percentageUsed", VmMetricCatalog.fieldName(SmartSeries.PERCENTAGE_USED));
        assertEquals("dataWrittenBytes", VmMetricCatalog.fieldName(SmartSeries.DATA_WRITTEN_BYTES));
        assertEquals("dataReadBytes", VmMetricCatalog.fieldName(SmartSeries.DATA_READ_BYTES));
        assertEquals("udmaCrcErrors", VmMetricCatalog.fieldName(SmartSeries.UDMA_CRC_ERRORS));
        assertEquals("offlineUncorrectable", VmMetricCatalog.fieldName(SmartSeries.OFFLINE_UNCORRECTABLE));
        assertEquals("startStopCounts", VmMetricCatalog.fieldName(SmartSeries.START_STOP_COUNTS));
        assertEquals("enduranceUsedPercent", VmMetricCatalog.fieldName(SmartSeries.ENDURANCE_USED_PERCENT));
        assertEquals("smartDevices", MetricFamily.SMART.section().jsonKey());
        assertEquals(List.of("device"), List.of(MetricFamily.SMART.groupingLabels()));
    }

    @Test
    void dockerIoSeries_mapToContainerSectionFields() {
        assertEquals(MetricFamily.DOCKER, VmMetricCatalog.familyOf(DockerSeries.NETWORK_RX_BYTES_PER_SECOND.metricName()));
        assertEquals("networkRxBytesPerSecond", VmMetricCatalog.fieldName(DockerSeries.NETWORK_RX_BYTES_PER_SECOND));
        assertEquals("blockWriteBytesPerSecond", VmMetricCatalog.fieldName(DockerSeries.BLOCK_WRITE_BYTES_PER_SECOND));
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
