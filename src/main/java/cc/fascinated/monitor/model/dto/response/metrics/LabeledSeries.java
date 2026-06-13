package cc.fascinated.monitor.model.dto.response.metrics;

import cc.fascinated.monitor.metrics.vm.query.Promql;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LabeledSeries(LinkedHashMap<String, Object> fields) {
    @JsonValue
    public LinkedHashMap<String, Object> json() {
        return this.fields;
    }

    public static LabeledSeries create(Map<String, String> labels, String[] identityLabels) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        for (String label : identityLabels) {
            fields.put(labelJsonKey(label), labels.get(label));
        }
        return new LabeledSeries(fields);
    }

    public void putSeries(String fieldName, List<Double> values) {
        Object existing = this.fields.get(fieldName);
        if (existing instanceof List<?> existingValues) {
            this.fields.put(fieldName, mergeValues(existingValues, values));
        } else {
            this.fields.put(fieldName, values);
        }
    }

    private static List<Double> mergeValues(List<?> left, List<Double> right) {
        List<Double> merged = new ArrayList<>(left.size());
        for (int index = 0; index < left.size(); index++) {
            Double leftValue = (Double) left.get(index);
            Double rightValue = right.get(index);
            merged.add(leftValue != null ? leftValue : rightValue);
        }
        return merged;
    }

    public void refreshLabels(Map<String, String> labels, String[] identityLabels) {
        for (String label : identityLabels) {
            String value = labels.get(label);
            if (value != null) {
                this.fields.put(labelJsonKey(label), value);
            }
        }
    }

    private static String labelJsonKey(String label) {
        return Promql.toCamelCase(label);
    }
}
