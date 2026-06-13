package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.server.catalog.ComputedMetric;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.vm.assembler.TimeSeriesAssembly;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.dto.response.metrics.LabeledSeries;
import cc.fascinated.monitor.model.dto.response.metrics.MetricsResponse;
import cc.fascinated.monitor.model.dto.response.metrics.ServerMetricsResponse;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VmMetricsAssembler {
    private VmMetricsAssembler() {}

    public static ServerMetricsResponse assemble(long id, MetricTimeRange range, MetricTimeRange.QueryWindow window,
                                                 List<VmTimeSeries> gauges,
                                                 Map<ComputedMetric, List<VmTimeSeries>> computed) {
        MetricTimeGrid grid = MetricTimeGrid.from(window);
        Map<MetricSection, Map<String, List<Double>>> scalars = new EnumMap<>(MetricSection.class);
        Map<MetricSection, Map<String, LabeledSeries>> labeled = new EnumMap<>(MetricSection.class);

        for (VmTimeSeries entry : gauges) {
            String metricName = entry.labels().get("__name__");
            if (metricName == null) {
                continue;
            }
            MetricFamily family = VmMetricCatalog.familyOf(metricName);
            if (family == null) {
                continue;
            }
            if (family.section().scalar()) {
                ingestScalar(grid, scalars, family, entry, metricName);
            } else {
                ingestLabeled(grid, labeled, family, entry, metricName, null);
            }
        }

        for (Map.Entry<ComputedMetric, List<VmTimeSeries>> computedEntry : computed.entrySet()) {
            ComputedMetric metric = computedEntry.getKey();
            for (VmTimeSeries entry : computedEntry.getValue()) {
                ingestLabeled(grid, labeled, metric.family(), entry, null, metric.fieldName());
            }
        }

        Map<String, Object> sections = new LinkedHashMap<>();
        for (MetricSection section : MetricSection.values()) {
            Object data = section.scalar()
                    ? TimeSeriesAssembly.pruneScalar(scalars.get(section))
                    : TimeSeriesAssembly.pruneLabeled(labeled.get(section));
            if (data != null) {
                sections.put(section.jsonKey(), data);
            }
        }

        List<Long> timestamps = sections.isEmpty() ? null : grid.timestamps();
        Long step = sections.isEmpty() ? null : range.step().getSeconds();
        return new ServerMetricsResponse(id, new MetricsResponse(range.param(), step, timestamps, Map.copyOf(sections)));
    }

    private static void ingestScalar(MetricTimeGrid grid, Map<MetricSection, Map<String, List<Double>>> scalars,
                                     MetricFamily family, VmTimeSeries entry, String metricName) {
        String name = VmMetricCatalog.fieldName(metricName);
        if (name == null) {
            return;
        }
        Map<String, List<Double>> fields = scalars.computeIfAbsent(family.section(), ignored -> new LinkedHashMap<>());
        fields.put(name, grid.align(entry));
    }

    private static void ingestLabeled(MetricTimeGrid grid, Map<MetricSection, Map<String, LabeledSeries>> labeled,
                                      MetricFamily family, VmTimeSeries entry, String metricName, String fieldOverride) {
        String name = fieldOverride;
        if (name == null) {
            if (metricName == null) {
                return;
            }
            name = VmMetricCatalog.fieldName(metricName);
            if (name == null) {
                return;
            }
        }
        Map<String, LabeledSeries> entities = labeled.computeIfAbsent(family.section(), ignored -> new LinkedHashMap<>());
        TimeSeriesAssembly.ingestLabeled(
                grid,
                entities,
                entry,
                name,
                family.groupingLabels(),
                family.identityLabels(),
                true
        );
    }
}
