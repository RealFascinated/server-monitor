package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.catalog.ComputedMetric;
import cc.fascinated.monitor.metrics.vm.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmQueryResponse;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.dto.response.server.metrics.ServerMetricsResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
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

    public ServerMetricsResponse readDashboard(long serverId, MetricTimeRange range) {
        MetricTimeRange.QueryWindow window = range.queryWindow();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<VmTimeSeries>> gauges = CompletableFuture.supplyAsync(
                    () -> fetchRange(VmMetricCatalog.selectorForServer(serverId), range, window),
                    executor
            );
            Map<ComputedMetric, CompletableFuture<List<VmTimeSeries>>> computedTasks =
                    new EnumMap<>(ComputedMetric.class);
            for (ComputedMetric computed : ComputedMetric.values()) {
                computedTasks.put(computed, CompletableFuture.supplyAsync(
                        () -> fetchRange(computed.promql(serverId), range, window),
                        executor
                ));
            }
            Map<ComputedMetric, List<VmTimeSeries>> computed = new EnumMap<>(ComputedMetric.class);
            for (ComputedMetric metric : ComputedMetric.values()) {
                computed.put(metric, computedTasks.get(metric).join());
            }
            return VmMetricsAssembler.assemble(serverId, range, window, gauges.join(), computed);
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
                VictoriaMetricsQuery.builder()
                        .metrics(metrics)
                        .serverIds(serverIds)
                        .build()
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
            Map<String, String> labelFilter = labelFiltersByMetric.getOrDefault(metric, Map.of());
            if (!matchesLabelFilter(series.labels(), labelFilter)) {
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

    private List<VmTimeSeries> fetchRange(String promql, MetricTimeRange range, MetricTimeRange.QueryWindow window) {
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(range.toRangeQuery(promql, window));
        return response.timeSeries();
    }

    private static boolean matchesLabelFilter(Map<String, String> labels, Map<String, String> filter) {
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            if (!entry.getValue().equals(labels.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
