package cc.fascinated.monitor.service;

import cc.fascinated.monitor.metrics.vm.MetricTimeGrid;
import cc.fascinated.monitor.metrics.platform.PlatformMetricsAssembler;
import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmQueryResponse;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.dto.response.metrics.MetricsResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminMetricsService {
    private final VictoriaMetricsQueryClient victoriaMetricsQueryClient;

    public AdminMetricsService(VictoriaMetricsQueryClient victoriaMetricsQueryClient) {
        this.victoriaMetricsQueryClient = victoriaMetricsQueryClient;
    }

    public MetricsResponse getMetrics(MetricQueryWindow window) {
        Map<PlatformMetricFamily, List<VmTimeSeries>> seriesByFamily = new EnumMap<>(PlatformMetricFamily.class);
        for (PlatformMetricFamily family : PlatformMetricFamily.values()) {
            if (family.histogram() && family.labeled()) {
                continue;
            }
            List<VmTimeSeries> combined = new ArrayList<>();
            for (String promql : family.adminRangeQueries(window.span())) {
                combined.addAll(fetchRange(promql, window));
            }
            seriesByFamily.put(family, combined);
        }
        Map<String, Object> sections = PlatformMetricsAssembler.assembleSections(window, seriesByFamily);
        List<Long> timestamps = sections.isEmpty() ? null : MetricTimeGrid.from(window).timestamps();
        Long step = sections.isEmpty() ? null : window.stepSeconds();
        return new MetricsResponse(window.fromEpoch(), window.toEpoch(), step, timestamps, sections);
    }

    private List<VmTimeSeries> fetchRange(String promql, MetricQueryWindow window) {
        VmQueryResponse response = this.victoriaMetricsQueryClient.execute(window.toRangeQuery(promql));
        return response.timeSeries();
    }
}
