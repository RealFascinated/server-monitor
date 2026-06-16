package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;

import java.util.OptionalDouble;
import java.util.OptionalLong;

@UtilityClass
public class PrometheusTextMetrics {

    public static OptionalLong sumGaugeValues(String body, String metricName) {
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
