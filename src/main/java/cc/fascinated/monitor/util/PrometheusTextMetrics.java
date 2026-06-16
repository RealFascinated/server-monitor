package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;

import java.util.OptionalDouble;
import java.util.OptionalLong;

@UtilityClass
public class PrometheusTextMetrics {

    public static OptionalLong sumGaugeValues(String body, String metricName) {
        return sumGaugeValues(body, metricName, null, null, false);
    }

    public static OptionalLong sumGaugeValuesExcludingLabelPrefix(String body, String metricName,
                                                                  String labelName, String excludedLabelValuePrefix) {
        return sumGaugeValues(body, metricName, labelName, excludedLabelValuePrefix, true);
    }

    private static OptionalLong sumGaugeValues(String body, String metricName, String labelName,
                                               String labelValuePrefix, boolean excludeLabelPrefix) {
        double sum = 0;
        boolean found = false;
        for (int index = 0; index < body.length(); ) {
            int lineEnd = body.indexOf('\n', index);
            if (lineEnd < 0) {
                lineEnd = body.length();
            }
            String line = body.substring(index, lineEnd);
            index = lineEnd + 1;

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            if (labelName != null) {
                String labelValue = labelValue(line, labelName);
                if (labelValue != null && labelValue.startsWith(labelValuePrefix) == excludeLabelPrefix) {
                    continue;
                }
            }
            OptionalDouble value = gaugeValue(line, metricName);
            if (value.isPresent()) {
                sum += value.getAsDouble();
                found = true;
            }
        }
        if (!found) {
            return OptionalLong.empty();
        }
        return OptionalLong.of((long) sum);
    }

    private static String labelValue(String line, String labelName) {
        int braceStart = line.indexOf('{');
        if (braceStart < 0) {
            return null;
        }
        int braceEnd = line.indexOf('}', braceStart);
        if (braceEnd < 0) {
            return null;
        }
        String labels = line.substring(braceStart + 1, braceEnd);
        for (String part : labels.split(",")) {
            String trimmed = part.trim();
            int equals = trimmed.indexOf('=');
            if (equals < 0 || !trimmed.substring(0, equals).trim().equals(labelName)) {
                continue;
            }
            String value = trimmed.substring(equals + 1).trim();
            if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
        return null;
    }

    private static OptionalDouble gaugeValue(String line, String metricName) {
        if (!line.startsWith(metricName)) {
            return OptionalDouble.empty();
        }
        int position = metricName.length();
        if (position < line.length() && line.charAt(position) == '{') {
            position = line.indexOf('}', position);
            if (position < 0) {
                return OptionalDouble.empty();
            }
            position++;
        } else if (position < line.length() && !Character.isWhitespace(line.charAt(position))) {
            return OptionalDouble.empty();
        }
        while (position < line.length() && Character.isWhitespace(line.charAt(position))) {
            position++;
        }
        int valueStart = position;
        while (position < line.length() && !Character.isWhitespace(line.charAt(position))) {
            position++;
        }
        if (valueStart == position) {
            return OptionalDouble.empty();
        }
        String valueText = line.substring(valueStart, position);
        if ("NaN".equalsIgnoreCase(valueText) || valueText.contains("Inf")) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(valueText));
        } catch (NumberFormatException ex) {
            return OptionalDouble.empty();
        }
    }
}
