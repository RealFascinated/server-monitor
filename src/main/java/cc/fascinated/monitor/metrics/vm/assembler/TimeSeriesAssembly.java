package cc.fascinated.monitor.metrics.vm.assembler;

import cc.fascinated.monitor.metrics.vm.MetricTimeGrid;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.dto.response.server.metrics.LabeledSeries;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class TimeSeriesAssembly {

    public static void ingestLabeled(MetricTimeGrid grid, Map<String, LabeledSeries> entities, VmTimeSeries entry,
                                     String fieldName, String[] groupingLabels, String[] identityLabels,
                                     boolean refreshLabels) {
        String key = identityKey(entry.labels(), groupingLabels);
        LabeledSeries entity = entities.computeIfAbsent(
                key,
                ignored -> LabeledSeries.create(entry.labels(), identityLabels)
        );
        if (refreshLabels) {
            entity.refreshLabels(entry.labels(), identityLabels);
        }
        entity.putSeries(fieldName, grid.align(entry));
    }

    public static String identityKey(Map<String, String> labels, String[] identityLabels) {
        if (identityLabels.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String label : identityLabels) {
            builder.append(label).append('=').append(labels.getOrDefault(label, "")).append('|');
        }
        return builder.toString();
    }

    public static Map<String, List<Double>> pruneScalar(Map<String, List<Double>> fields) {
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

    public static List<LabeledSeries> pruneLabeled(Map<String, LabeledSeries> entities) {
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

    public static boolean hasData(List<Double> values) {
        for (Double value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSeriesData(LabeledSeries item) {
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
