package cc.fascinated.monitor.metrics.server.catalog;

import cc.fascinated.monitor.metrics.vm.query.Promql;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.server.series.CpuCoreSeries;
import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.SmartSeries;
import cc.fascinated.monitor.metrics.server.series.DockerSeries;
import cc.fascinated.monitor.metrics.server.series.GpuSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import cc.fascinated.monitor.metrics.server.series.NetworkSeries;
import cc.fascinated.monitor.metrics.server.series.PsiSeries;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.metrics.server.series.TemperatureSeries;
import cc.fascinated.monitor.metrics.server.series.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsArcSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsPoolSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
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
    private static final Map<String, VmGaugeSeries> ALERTABLE_BY_METRIC = buildAlertable();

    public static MetricFamily familyOf(String metricName) {
        return FAMILY_BY_METRIC.get(metricName);
    }

    /** Series users can alert on, keyed by metric name. */
    public static Map<String, VmGaugeSeries> alertable() {
        return ALERTABLE_BY_METRIC;
    }

    /** Alert metadata for a metric name; falls back to the raw name for a series that is no longer alertable. */
    public static AlertInfo alertInfoOf(String metricName) {
        VmGaugeSeries series = ALERTABLE_BY_METRIC.get(metricName);
        AlertInfo info = series == null ? null : series.alertInfo();
        return info != null ? info : AlertInfo.of(metricName, AlertUnit.NONE, AlertOperator.MORE_THAN, 0);
    }

    public static List<String> identityLabelsOf(String metricName) {
        MetricFamily family = FAMILY_BY_METRIC.get(metricName);
        return family == null ? List.of() : List.of(family.identityLabels());
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
        addFamily(metrics, PsiSeries.values());
        addFamily(metrics, CpuCoreSeries.values());
        addFamily(metrics, DiskSeries.values());
        addFamily(metrics, NetworkSeries.values());
        addFamily(metrics, GpuSeries.values());
        addFamily(metrics, DockerSeries.values());
        addFamily(metrics, TemperatureSeries.values());
        addFamily(metrics, ZfsArcSeries.values());
        addFamily(metrics, ZfsPoolSeries.values());
        addFamily(metrics, TcpConnectionSeries.values());
        addFamily(metrics, SmartSeries.values());
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

    private static Map<String, VmGaugeSeries> buildAlertable() {
        Map<String, VmGaugeSeries> map = new HashMap<>();
        for (VmGaugeSeries series : ALL_FAMILIES) {
            if (series.alertInfo() != null) {
                map.put(series.metricName(), series);
            }
        }
        for (VmGaugeSeries series : ServerStatusSeries.values()) {
            if (series.alertInfo() != null) {
                map.put(series.metricName(), series);
            }
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
