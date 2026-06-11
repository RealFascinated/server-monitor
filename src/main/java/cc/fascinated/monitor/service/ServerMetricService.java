package cc.fascinated.monitor.service;

import cc.fascinated.monitor.metrics.vm.ServerMetricGroups;
import cc.fascinated.monitor.metrics.vm.VmMetricsAssembler;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmQueryResponse;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.dto.response.server.metrics.ServerMetricsResponse;
import cc.fascinated.monitor.model.persistance.ServerRow;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ServerMetricService {
    private final VictoriaMetricsQueryClient victoriaMetricsQueryClient;

    public ServerMetricService(VictoriaMetricsQueryClient victoriaMetricsQueryClient) {
        this.victoriaMetricsQueryClient = victoriaMetricsQueryClient;
    }

    public ServerMetricsResponse getServerMetrics(ServerRow server, MetricTimeRange range) {
        MetricTimeRange.QueryWindow window = range.queryWindow();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<ServerMetricGroups, CompletableFuture<List<VmTimeSeries>>> tasks = new EnumMap<>(ServerMetricGroups.class);
            for (ServerMetricGroups group : ServerMetricGroups.values()) {
                tasks.put(group, CompletableFuture.supplyAsync(
                        () -> fetchGroup(server.getId(), range, window, group),
                        executor
                ));
            }
            Map<ServerMetricGroups, List<VmTimeSeries>> byGroup = new EnumMap<>(ServerMetricGroups.class);
            for (ServerMetricGroups group : ServerMetricGroups.values()) {
                byGroup.put(group, tasks.get(group).join());
            }
            return VmMetricsAssembler.assemble(server.getId(), range, window, byGroup);
        }
    }

    public Map<Long, Double> fetchLatestMetric(VmGaugeSeries metric, List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return Map.of();
        }
        String ids = serverIds.stream().map(String::valueOf).collect(Collectors.joining("|"));
        String promql = metric.metricName() + "{server_id=~\"" + ids + "\"}";
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(
                VictoriaMetricsQuery.builder().query(promql).build()
        );
        Map<Long, Double> result = new HashMap<>();
        for (VmTimeSeries series : response.timeSeries()) {
            String serverIdLabel = series.labels().get("server_id");
            if (serverIdLabel == null || series.samples().isEmpty()) {
                continue;
            }
            result.put(Long.parseLong(serverIdLabel), series.samples().getLast().value());
        }
        return result;
    }

    private List<VmTimeSeries> fetchGroup(long serverId, MetricTimeRange range, MetricTimeRange.QueryWindow window,
                                          ServerMetricGroups group) {
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(
                range.toRangeQuery(group.promql(serverId), window)
        );
        return response.timeSeries();
    }
}
