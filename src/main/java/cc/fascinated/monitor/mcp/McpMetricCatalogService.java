package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.alert.SeriesKeys;
import cc.fascinated.monitor.mcp.dto.McpMetricInfo;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.vm.MetricSection;
import cc.fascinated.monitor.metrics.vm.query.Promql;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The metric ids a client can query. This is wider than {@link VmMetricCatalog#alertable()}: every stored gauge is
 * queryable, but only some can be alerted on.
 */
@Service
public class McpMetricCatalogService {
    private static final String HOST_GROUP = "host";

    /** Every gauge a server can report: the metric families plus the up/down status series. */
    private static final List<String> QUERYABLE = Stream.concat(
            VmMetricCatalog.allGaugeNames().stream(),
            Arrays.stream(ServerStatusSeries.values()).map(VmGaugeSeries::metricName)
    ).toList();
    private static final Set<String> QUERYABLE_SET = Set.copyOf(QUERYABLE);

    private final ServerService serverService;
    private final VictoriaMetricsQueryClient queryClient;

    public McpMetricCatalogService(ServerService serverService, VictoriaMetricsQueryClient queryClient) {
        this.serverService = serverService;
        this.queryClient = queryClient;
    }

    public static boolean isQueryable(String metric) {
        return QUERYABLE_SET.contains(metric);
    }

    /**
     * Rejects a metric a rule cannot target with a message that says where to look, since the rule validator's own
     * "Unknown metric" is written for the dashboard and is wrong for a queryable but non-alertable metric.
     */
    public static void requireAlertable(String metric) {
        if (VmMetricCatalog.alertable().containsKey(metric)) {
            return;
        }
        String problem = isQueryable(metric) ? "\"%s\" can be queried but not alerted on" : "Unknown metric \"%s\"";
        throw new BadRequestException((problem + ". Use list_metrics and pick a metric with alertable=true")
                .formatted(metric));
    }

    /** Label keys a filter on {@code metric} may use. */
    public static List<String> labelKeysOf(String metric) {
        return VmMetricCatalog.identityLabelsOf(metric);
    }

    public static String groupOf(String metric) {
        MetricFamily family = VmMetricCatalog.familyOf(metric);
        return family == null ? HOST_GROUP : family.section().jsonKey();
    }

    /**
     * @param group    only metrics of this group (e.g. {@code containers}), or all
     * @param metrics  only these metric ids, or all
     * @param serverId only metrics this server reports now; needs VIEW_METRICS
     * @param includeSeries with a {@code serverId}, also the label sets it has for each metric. Off by default
     *                      because every label value of every group is a lot of output
     */
    public List<McpMetricInfo> list(UserRow user, @Nullable String group, @Nullable List<String> metrics,
                                    @Nullable Long serverId, boolean includeSeries) {
        if (group != null && !groups().contains(group)) {
            throw new BadRequestException("Unknown group \"%s\". Valid groups: %s"
                    .formatted(group, String.join(", ", groups())));
        }
        Set<String> wanted = metrics == null || metrics.isEmpty() ? null : Set.copyOf(metrics);
        if (wanted != null) {
            for (String metric : wanted) {
                if (!isQueryable(metric)) {
                    throw new BadRequestException("Unknown metric \"%s\". Use list_metrics to see valid metric ids"
                            .formatted(metric));
                }
            }
        }
        Map<String, Set<Map<String, String>>> reported = null;
        if (serverId != null) {
            this.serverService.requireServer(user, serverId, ServerPermission.VIEW_METRICS);
            reported = reportedSeries(serverId);
        }
        List<McpMetricInfo> result = new ArrayList<>();
        for (String metric : QUERYABLE) {
            if (group != null && !group.equals(groupOf(metric))) {
                continue;
            }
            if (wanted != null && !wanted.contains(metric)) {
                continue;
            }
            if (reported != null && !reported.containsKey(metric)) {
                continue;
            }
            result.add(toInfo(metric, reported == null || !includeSeries ? null : reported.get(metric)));
        }
        result.sort(Comparator.comparing(McpMetricInfo::group).thenComparing(McpMetricInfo::metric));
        return result;
    }

    private static McpMetricInfo toInfo(String metric, @Nullable Set<Map<String, String>> series) {
        List<String> labelKeys = labelKeysOf(metric);
        VmGaugeSeries alertable = VmMetricCatalog.alertable().get(metric);
        AlertInfo info = alertable == null ? null : alertable.alertInfo();
        List<Map<String, String>> labelSets = series == null || labelKeys.isEmpty() ? null : series.stream()
                .sorted(Comparator.comparing(Object::toString))
                .toList();
        return new McpMetricInfo(metric, groupOf(metric), labelKeys, info != null,
                info == null ? null : info.label(), info == null ? null : info.unit(),
                info == null ? null : info.operator(), info == null ? null : info.threshold(), labelSets);
    }

    private static List<String> groups() {
        return Arrays.stream(MetricSection.values()).map(MetricSection::jsonKey).toList();
    }

    /** Metric name to the identity-label sets a server reports right now, in one VictoriaMetrics query. */
    private Map<String, Set<Map<String, String>>> reportedSeries(long serverId) {
        String selector = Promql.vectorSelectorRegex(QUERYABLE, Map.of("server_id", Long.toString(serverId)));
        List<VmTimeSeries> series = this.queryClient.execute(VictoriaMetricsQuery.builder().query(selector).build())
                .timeSeries();
        Map<String, Set<Map<String, String>>> byMetric = new TreeMap<>();
        for (VmTimeSeries entry : series) {
            String metric = entry.labels().get("__name__");
            if (metric == null) {
                continue;
            }
            Map<String, String> labels = SeriesKeys.seriesLabels(entry.labels());
            Map<String, String> identity = new TreeMap<>();
            for (String key : labelKeysOf(metric)) {
                if (labels.containsKey(key)) {
                    identity.put(key, labels.get(key));
                }
            }
            byMetric.computeIfAbsent(metric, ignored -> new LinkedHashSet<>()).add(identity);
        }
        return byMetric;
    }
}
