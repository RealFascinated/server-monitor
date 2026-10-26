package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VmMetricsReaderTest {
    @Test
    void latestMetricsPushesPerMetricLabelFiltersIntoOneSelector() {
        Map<String, String> rootDisk = Map.of("disk", "/");
        assertEquals(
                "{__name__=~\"monitor_host_cpu_usage|monitor_host_mem_usage\",server_id=~\"1|2\""
                        + " or __name__=~\"monitor_disk_used_bytes|monitor_disk_total_bytes\",disk=\"/\",server_id=~\"1|2\"}",
                VmMetricsReader.latestMetricsPromql(
                        List.of(HostSeries.CPU_USAGE, HostSeries.MEM_USAGE, DiskSeries.USED_BYTES, DiskSeries.TOTAL_BYTES),
                        List.of(1L, 2L),
                        Map.of(DiskSeries.USED_BYTES, rootDisk, DiskSeries.TOTAL_BYTES, rootDisk)
                )
        );
    }

    @Test
    void latestMetricsWithoutLabelFiltersIsASingleBranch() {
        assertEquals(
                "{__name__=~\"monitor_host_cpu_usage|monitor_host_mem_usage\",server_id=~\"7\"}",
                VmMetricsReader.latestMetricsPromql(
                        List.of(HostSeries.CPU_USAGE, HostSeries.MEM_USAGE), List.of(7L), Map.of())
        );
    }
}
