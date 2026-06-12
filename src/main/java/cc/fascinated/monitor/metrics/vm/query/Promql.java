package cc.fascinated.monitor.metrics.vm.query;

import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class Promql {
    public static String toCamelCase(String snakeCase) {
        StringBuilder builder = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
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

    public static void appendEscapedLabelValue(StringBuilder builder, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') {
                builder.append('\\');
            }
            builder.append(c);
        }
    }

    public static String serverIdsRegex(Collection<Long> serverIds) {
        return serverIds.stream().map(String::valueOf).collect(Collectors.joining("|"));
    }

    public static String metricNamesRegex(Collection<? extends VmGaugeSeries> metrics) {
        return metrics.stream().map(VmGaugeSeries::metricName).collect(Collectors.joining("|"));
    }

    public static String vectorSelector(String metricName, Map<String, String> labels) {
        if (labels.isEmpty()) {
            return metricName;
        }
        StringBuilder builder = new StringBuilder(metricName);
        builder.append('{');
        appendExactLabels(builder, labels);
        builder.append('}');
        return builder.toString();
    }

    public static String vectorSelectorRegex(Collection<String> metricNames, Map<String, String> exactLabels) {
        return vectorSelectorRegex(metricNames, exactLabels, Map.of());
    }

    public static String vectorSelectorRegex(
            Collection<String> metricNames,
            Map<String, String> exactLabels,
            Map<String, String> regexLabels
    ) {
        StringBuilder builder = new StringBuilder("{__name__=~\"");
        builder.append(String.join("|", metricNames));
        builder.append('"');
        appendExactLabels(builder, exactLabels);
        appendRegexLabels(builder, regexLabels);
        builder.append('}');
        return builder.toString();
    }

    public static String vectorSelectorRegex(
            String metricNamesRegex,
            Map<String, String> exactLabels,
            Map<String, String> regexLabels
    ) {
        StringBuilder builder = new StringBuilder("{__name__=~\"");
        builder.append(metricNamesRegex);
        builder.append('"');
        appendExactLabels(builder, exactLabels);
        appendRegexLabels(builder, regexLabels);
        builder.append('}');
        return builder.toString();
    }

    private static void appendExactLabels(StringBuilder builder, Map<String, String> labels) {
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            builder.append(',');
            builder.append(entry.getKey());
            builder.append("=\"");
            appendEscapedLabelValue(builder, entry.getValue());
            builder.append('"');
        }
    }

    private static void appendRegexLabels(StringBuilder builder, Map<String, String> labels) {
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            builder.append(',');
            builder.append(entry.getKey());
            builder.append("=~\"");
            appendEscapedLabelValue(builder, entry.getValue());
            builder.append('"');
        }
    }
}
