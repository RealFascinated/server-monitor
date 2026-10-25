package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.dto.response.alert.AlertMetricResponse;
import cc.fascinated.monitor.model.dto.response.alert.AlertSeriesResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Read-only helpers for the rule builder: the metric menu and the label sets a server is reporting. */
@Service
public class AlertSeriesService {
    private final VictoriaMetricsQueryClient queryClient;
    private final ServerService serverService;

    public AlertSeriesService(VictoriaMetricsQueryClient queryClient, ServerService serverService) {
        this.queryClient = queryClient;
        this.serverService = serverService;
    }

    public List<AlertMetricResponse> metrics() {
        return VmMetricCatalog.alertable().values().stream()
                .map(series -> AlertMetricResponse.from(series, series.alertInfo(), VmMetricCatalog.familyOf(series.metricName())))
                .sorted(Comparator.comparing(AlertMetricResponse::group).thenComparing(AlertMetricResponse::label))
                .toList();
    }

    public AlertSeriesResponse series(UserRow user, long serverId, String metric) {
        this.serverService.requireServer(user, serverId, ServerPermission.VIEW_METRICS);
        if (!VmMetricCatalog.alertable().containsKey(metric)) {
            throw new BadRequestException("Unknown metric \"%s\"".formatted(metric));
        }
        List<VmTimeSeries> series = this.queryClient.execute(
                VictoriaMetricsQuery.builder().metric(metric).serverId(serverId).build()).timeSeries();
        Set<Map<String, String>> labelSets = new LinkedHashSet<>();
        for (VmTimeSeries entry : series) {
            Map<String, String> labels = SeriesKeys.seriesLabels(entry.labels());
            Map<String, String> identity = new TreeMap<>();
            for (String key : VmMetricCatalog.identityLabelsOf(metric)) {
                if (labels.containsKey(key)) {
                    identity.put(key, labels.get(key));
                }
            }
            labelSets.add(identity);
        }
        return new AlertSeriesResponse(labelSets.stream()
                .sorted(Comparator.comparing(set -> set.toString()))
                .toList());
    }
}
