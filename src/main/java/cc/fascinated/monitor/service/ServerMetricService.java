package cc.fascinated.monitor.service;

import cc.fascinated.monitor.metrics.vm.VmMetricsReader;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.dto.response.metrics.ServerMetricsResponse;
import cc.fascinated.monitor.model.persistance.ServerRow;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ServerMetricService {
    private final VmMetricsReader vmMetricsReader;

    public ServerMetricService(VmMetricsReader vmMetricsReader) {
        this.vmMetricsReader = vmMetricsReader;
    }

    public ServerMetricsResponse getServerMetrics(ServerRow server, MetricQueryWindow window) {
        return this.vmMetricsReader.readDashboard(server.getId(), window);
    }

    public Map<Long, Double> fetchUptimePercent30d(List<Long> serverIds) {
        return this.vmMetricsReader.fetchUptimePercent30d(serverIds);
    }

    public Map<VmGaugeSeries, Map<Long, Double>> fetchLatestMetrics(
            List<? extends VmGaugeSeries> metrics,
            List<Long> serverIds,
            Map<? extends VmGaugeSeries, Map<String, String>> labelFiltersByMetric
    ) {
        return this.vmMetricsReader.fetchLatestMetrics(metrics, serverIds, labelFiltersByMetric);
    }
}
