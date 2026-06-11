package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.dto.response.server.metrics.LabeledSeries;
import cc.fascinated.monitor.model.dto.response.server.metrics.ServerMetricsResponse;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VmMetricsAssembler {
    private VmMetricsAssembler() {}

    public static ServerMetricsResponse assemble(long id, MetricTimeRange range, MetricTimeRange.QueryWindow window,
                                                 Map<ServerMetricGroups, List<VmTimeSeries>> byGroup) {
        MetricTimeGrid grid = MetricTimeGrid.from(window);
        Map<MetricSection, Map<String, List<Double>>> scalars = new EnumMap<>(MetricSection.class);
        Map<MetricSection, Map<String, LabeledSeries>> labeled = new EnumMap<>(MetricSection.class);

        for (ServerMetricGroups group : ServerMetricGroups.values()) {
            List<VmTimeSeries> series = byGroup.get(group);
            if (series == null || series.isEmpty()) {
                continue;
            }
            if (group.mergeField() != null) {
                ingestLabeled(grid, labeled, group.section(), series, group.identityLabels(), group.mergeField());
            } else if (group.section().scalar()) {
                ingestScalar(grid, scalars, group.section(), series);
            } else {
                ingestLabeled(grid, labeled, group.section(), series, group.identityLabels(), null);
            }
        }

        Map<String, Object> sections = new LinkedHashMap<>();
        for (MetricSection section : MetricSection.values()) {
            Object data = section.scalar()
                    ? pruneScalar(scalars.get(section))
                    : pruneLabeled(labeled.get(section));
            if (data != null) {
                sections.put(section.jsonKey(), data);
            }
        }

        List<Long> timestamps = sections.isEmpty() ? null : grid.timestamps();
        Long step = sections.isEmpty() ? null : range.step().getSeconds();
        return new ServerMetricsResponse(id, range.param(), step, timestamps, Map.copyOf(sections));
    }

    private static void ingestScalar(MetricTimeGrid grid, Map<MetricSection, Map<String, List<Double>>> scalars,
                                     MetricSection section, List<VmTimeSeries> series) {
        Map<String, List<Double>> fields = scalars.computeIfAbsent(section, ignored -> new LinkedHashMap<>());
        for (VmTimeSeries entry : series) {
            String metricName = entry.labels().get("__name__");
            if (metricName == null) {
                continue;
            }
            String name = MetricFieldNames.toFieldName(metricName);
            if (name != null) {
                fields.put(name, grid.align(entry));
            }
        }
    }

    private static void ingestLabeled(MetricTimeGrid grid, Map<MetricSection, Map<String, LabeledSeries>> labeled,
                                    MetricSection section, List<VmTimeSeries> series, String[] identityLabels, String fieldOverride) {
        Map<String, LabeledSeries> entities = labeled.computeIfAbsent(section, ignored -> new LinkedHashMap<>());
        for (VmTimeSeries entry : series) {
            String name = fieldOverride;
            if (name == null) {
                String metricName = entry.labels().get("__name__");
                if (metricName == null) {
                    continue;
                }
                name = MetricFieldNames.toFieldName(metricName);
                if (name == null) {
                    continue;
                }
            }
            String key = identityKey(entry.labels(), identityLabels);
            LabeledSeries entity = entities.computeIfAbsent(key, ignored -> LabeledSeries.create(entry.labels(), identityLabels));
            entity.putSeries(name, grid.align(entry));
        }
    }

    private static String identityKey(Map<String, String> labels, String[] identityLabels) {
        if (identityLabels.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String label : identityLabels) {
            builder.append(label).append('=').append(labels.getOrDefault(label, "")).append('|');
        }
        return builder.toString();
    }

    private static Map<String, List<Double>> pruneScalar(Map<String, List<Double>> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        Map<String, List<Double>> pruned = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : fields.entrySet()) {
            if (hasData(entry.getValue())) {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        return pruned.isEmpty() ? null : pruned;
    }

    private static List<LabeledSeries> pruneLabeled(Map<String, LabeledSeries> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        List<LabeledSeries> pruned = new ArrayList<>();
        for (LabeledSeries entity : entities.values()) {
            if (hasSeriesData(entity)) {
                pruned.add(entity);
            }
        }
        return pruned.isEmpty() ? null : pruned;
    }

    private static boolean hasData(List<Double> values) {
        for (Double value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSeriesData(LabeledSeries item) {
        for (Object value : item.fields().values()) {
            if (value instanceof List<?> list) {
                for (Object element : list) {
                    if (element != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
