package cc.fascinated.monitor.metrics.vm.catalog;

import cc.fascinated.monitor.metrics.vm.query.Promql;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.CpuCoreSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DiskSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DockerSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.GpuSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.HostSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.NetworkSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ServerStatusSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TemperatureSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsArcSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsPoolSeries;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class VmMetricCatalog {
    private static final VmMetricFamily[] ALL_FAMILIES = concatFamilies();
    private static final Map<String, MetricFamily> FAMILY_BY_METRIC = buildFamilyByMetric();
    private static final Map<String, String> FIELD_BY_METRIC = buildFieldByMetric();
    private static final List<String> ALL_GAUGE_NAMES = buildAllGaugeNames();

    public static MetricFamily familyOf(String metricName) {
        return FAMILY_BY_METRIC.get(metricName);
    }

    public static String fieldName(VmGaugeSeries metric) {
        return fieldName(metric.metricName());
    }

    public static String fieldName(String metricName) {
        return FIELD_BY_METRIC.get(metricName);
    }

    public static List<String> allGaugeNames() {
        return ALL_GAUGE_NAMES;
    }

    public static String selectorForServer(long serverId) {
        return Promql.vectorSelectorRegex(ALL_GAUGE_NAMES, Map.of("server_id", Long.toString(serverId)));
    }

    public static String selectorForServers(Collection<Long> serverIds, Collection<? extends VmGaugeSeries> metrics) {
        List<String> names = metrics.stream().map(VmGaugeSeries::metricName).toList();
        return Promql.vectorSelectorRegex(
                names,
                Map.of(),
                Map.of("server_id", Promql.serverIdsRegex(serverIds))
        );
    }

    public static String uptimePromql(Collection<Long> serverIds) {
        return "avg_over_time(%s{server_id=~\"%s\"}[30d]) * 100".formatted(
                ServerStatusSeries.UP.metricName(),
                Promql.serverIdsRegex(serverIds)
        );
    }

    private static VmMetricFamily[] concatFamilies() {
        List<VmMetricFamily> metrics = new ArrayList<>();
        addFamily(metrics, HostSeries.values());
        addFamily(metrics, CpuCoreSeries.values());
        addFamily(metrics, DiskSeries.values());
        addFamily(metrics, NetworkSeries.values());
        addFamily(metrics, GpuSeries.values());
        addFamily(metrics, DockerSeries.values());
        addFamily(metrics, TemperatureSeries.values());
        addFamily(metrics, ZfsArcSeries.values());
        addFamily(metrics, ZfsPoolSeries.values());
        addFamily(metrics, TcpConnectionSeries.values());
        return metrics.toArray(VmMetricFamily[]::new);
    }

    private static void addFamily(List<VmMetricFamily> metrics, VmMetricFamily[] values) {
        for (VmMetricFamily metric : values) {
            metrics.add(metric);
        }
    }

    private static Map<String, MetricFamily> buildFamilyByMetric() {
        Map<String, MetricFamily> map = new HashMap<>();
        for (VmMetricFamily metric : ALL_FAMILIES) {
            map.put(metric.metricName(), metric.metricFamily());
        }
        return Map.copyOf(map);
    }

    private static Map<String, String> buildFieldByMetric() {
        Map<String, String> map = new HashMap<>();
        for (VmMetricFamily metric : ALL_FAMILIES) {
            String metricName = metric.metricName();
            String prefix = metric.metricPrefix();
            if (!metricName.startsWith(prefix)) {
                continue;
            }
            map.put(metricName, Promql.toCamelCase(metricName.substring(prefix.length())));
        }
        return Map.copyOf(map);
    }

    private static List<String> buildAllGaugeNames() {
        List<String> names = new ArrayList<>(ALL_FAMILIES.length);
        for (VmMetricFamily metric : ALL_FAMILIES) {
            names.add(metric.metricName());
        }
        return List.copyOf(names);
    }
}
