package cc.fascinated.monitor.model.dto.response.server.metrics;

import com.fasterxml.jackson.annotation.JsonValue;

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
        this.fields.put(fieldName, values);
    }

    private static String labelJsonKey(String label) {
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
