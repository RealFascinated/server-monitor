package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.server.catalog.ComputedMetric;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.vm.query.Promql;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmQueryResponse;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.dto.response.metrics.ServerMetricsResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class VmMetricsReader {
    private final VictoriaMetricsQueryClient victoriaMetricsQueryClient;

    public VmMetricsReader(VictoriaMetricsQueryClient victoriaMetricsQueryClient) {
        this.victoriaMetricsQueryClient = victoriaMetricsQueryClient;
    }

    public ServerMetricsResponse readDashboard(long serverId, MetricQueryWindow window) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<VmTimeSeries>> gauges = CompletableFuture.supplyAsync(
                    () -> fetchRange(VmMetricCatalog.selectorForServer(serverId), window),
                    executor
            );
            Map<ComputedMetric, CompletableFuture<List<VmTimeSeries>>> computedTasks =
                    new EnumMap<>(ComputedMetric.class);
            for (ComputedMetric computed : ComputedMetric.values()) {
                computedTasks.put(computed, CompletableFuture.supplyAsync(
                        () -> fetchRange(computed.promql(serverId), window),
                        executor
                ));
            }
            Map<ComputedMetric, List<VmTimeSeries>> computed = new EnumMap<>(ComputedMetric.class);
            for (ComputedMetric metric : ComputedMetric.values()) {
                computed.put(metric, computedTasks.get(metric).join());
            }
            return VmMetricsAssembler.assemble(serverId, window, gauges.join(), computed);
        }
    }

    public Map<Long, Double> fetchUptimePercent30d(List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return Map.of();
        }
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(
                VictoriaMetricsQuery.builder().query(VmMetricCatalog.uptimePromql(serverIds)).build()
        );
        Map<Long, Double> result = new HashMap<>();
        for (VmTimeSeries series : response.timeSeries()) {
            String serverIdLabel = series.labels().get("server_id");
            if (serverIdLabel == null || series.samples().isEmpty()) {
                continue;
            }
            double value = series.samples().getLast().value();
            if (Double.isFinite(value)) {
                result.put(Long.parseLong(serverIdLabel), value);
            }
        }
        return result;
    }

    public Map<VmGaugeSeries, Map<Long, Double>> fetchLatestMetrics(
            Collection<? extends VmGaugeSeries> metrics,
            List<Long> serverIds,
            Map<? extends VmGaugeSeries, Map<String, String>> labelFiltersByMetric
    ) {
        if (serverIds.isEmpty() || metrics.isEmpty()) {
            return Map.of();
        }
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(
                VictoriaMetricsQuery.builder().query(latestMetricsPromql(metrics, serverIds, labelFiltersByMetric)).build()
        );
        Map<String, VmGaugeSeries> byName = new HashMap<>();
        for (VmGaugeSeries metric : metrics) {
            byName.put(metric.metricName(), metric);
        }
        Map<VmGaugeSeries, Map<Long, Double>> result = new HashMap<>();
        for (VmTimeSeries series : response.timeSeries()) {
            String metricName = series.labels().get("__name__");
            VmGaugeSeries metric = byName.get(metricName);
            if (metric == null || series.samples().isEmpty()) {
                continue;
            }
            String serverIdLabel = series.labels().get("server_id");
            if (serverIdLabel == null) {
                continue;
            }
            result.computeIfAbsent(metric, ignored -> new HashMap<>())
                    .put(Long.parseLong(serverIdLabel), series.samples().getLast().value());
        }
        return result;
    }

    private List<VmTimeSeries> fetchRange(String promql, MetricQueryWindow window) {
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(window.toRangeQuery(promql));
        return response.timeSeries();
    }

    /**
     * Metrics are grouped by the label filter they need, and each group becomes one branch of a single
     * selector, so a filter like {@code disk="/"} is applied by VictoriaMetrics rather than after every
     * mount's series has been sent back.
     */
    static String latestMetricsPromql(
            Collection<? extends VmGaugeSeries> metrics,
            List<Long> serverIds,
            Map<? extends VmGaugeSeries, Map<String, String>> labelFiltersByMetric
    ) {
        Map<Map<String, String>, List<String>> namesByFilter = new LinkedHashMap<>();
        for (VmGaugeSeries metric : metrics) {
            namesByFilter.computeIfAbsent(labelFiltersByMetric.getOrDefault(metric, Map.of()), ignored -> new ArrayList<>())
                    .add(metric.metricName());
        }
        Map<String, String> serverFilter = Map.of("server_id", Promql.serverIdsRegex(serverIds));
        List<Promql.SelectorFilter> filters = new ArrayList<>(namesByFilter.size());
        namesByFilter.forEach((labels, names) -> filters.add(new Promql.SelectorFilter(names, labels, serverFilter)));
        return Promql.vectorSelectorAnyOf(filters);
    }
}
