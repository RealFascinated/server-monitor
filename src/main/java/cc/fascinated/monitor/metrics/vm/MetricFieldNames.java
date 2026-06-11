package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.CpuCoreSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DiskSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DockerSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.GpuSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.HostSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.NetworkSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TemperatureSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsArcSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsPoolSeries;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class MetricFieldNames {
    private static final Map<String, String> BY_METRIC_NAME = buildMap();

    public static String toFieldName(String metricName) {
        return BY_METRIC_NAME.get(metricName);
    }

    private static Map<String, String> buildMap() {
        Map<String, String> map = new HashMap<>();
        register(map, HostSeries.values(), "monitor_host_");
        register(map, CpuCoreSeries.values(), "monitor_host_");
        register(map, DiskSeries.values(), "monitor_disk_");
        register(map, NetworkSeries.values(), "monitor_net_");
        register(map, GpuSeries.values(), "monitor_gpu_");
        register(map, DockerSeries.values(), "monitor_container_");
        register(map, TemperatureSeries.values(), "monitor_host_");
        register(map, ZfsArcSeries.values(), "monitor_zfs_arc_");
        register(map, ZfsPoolSeries.values(), "monitor_zfs_pool_");
        register(map, TcpConnectionSeries.values(), "monitor_tcp_");
        return Map.copyOf(map);
    }

    private static void register(Map<String, String> map, VmGaugeSeries[] series, String prefix) {
        for (VmGaugeSeries metric : series) {
            String metricName = metric.metricName();
            if (!metricName.startsWith(prefix)) {
                continue;
            }
            map.put(metricName, toCamelCase(metricName.substring(prefix.length())));
        }
    }

    private static String toCamelCase(String snakeCase) {
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
