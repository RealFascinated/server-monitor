package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.engine.RuleSpec;
import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Checks the evaluation part of a rule, shared by rule create/update and the preview endpoint. */
@Component
public class AlertRuleValidator {
    private static final int MIN_WINDOW_SECONDS = 10;
    private static final int MAX_LABEL_FILTERS = 5;
    private static final int MAX_LABEL_VALUE_LENGTH = 128;

    private final MonitorAlertProperties properties;

    public AlertRuleValidator(MonitorAlertProperties properties) {
        this.properties = properties;
    }

    /** The normalised, validated evaluation part of a rule. */
    public record Definition(String metric, Map<String, String> labelFilter, RuleSpec spec) {}

    public Definition validate(String metric, @Nullable Map<String, String> labelFilter, AlertOperator operator,
                               double threshold, @Nullable Double recoveryThreshold, @Nullable Integer windowSeconds) {
        if (!VmMetricCatalog.alertable().containsKey(metric)) {
            throw new BadRequestException("Unknown metric \"%s\"".formatted(metric));
        }
        Map<String, String> filter = validateLabelFilter(metric, labelFilter);
        if (!Double.isFinite(threshold)) {
            throw new BadRequestException("Threshold must be a finite number");
        }
        if (recoveryThreshold != null) {
            if (!Double.isFinite(recoveryThreshold)) {
                throw new BadRequestException("Recovery threshold must be a finite number");
            }
            boolean wrongSide = operator == AlertOperator.MORE_THAN
                    ? recoveryThreshold > threshold
                    : recoveryThreshold < threshold;
            if (wrongSide) {
                throw new BadRequestException(operator == AlertOperator.MORE_THAN
                        ? "Recovery threshold must not be above the threshold"
                        : "Recovery threshold must not be below the threshold");
            }
        }
        int window = windowSeconds == null ? 0 : windowSeconds;
        if (ServerStatusSeries.UP.metricName().equals(metric)) {
            if (operator != AlertOperator.LESS_THAN || threshold <= 0 || threshold > 1) {
                throw new BadRequestException("A server offline alert must be below a threshold between 0 and 1");
            }
            window = 0;
            recoveryThreshold = null;
        } else if (window != 0 && (window < MIN_WINDOW_SECONDS || window > this.properties.getMaxWindow().toSeconds())) {
            throw new BadRequestException("Window must be off or between %d seconds and %d seconds".formatted(
                    MIN_WINDOW_SECONDS, this.properties.getMaxWindow().toSeconds()));
        }
        return new Definition(metric, filter, new RuleSpec(operator, threshold, recoveryThreshold, window));
    }

    private Map<String, String> validateLabelFilter(String metric, @Nullable Map<String, String> labelFilter) {
        if (labelFilter == null || labelFilter.isEmpty()) {
            return Map.of();
        }
        List<String> allowed = VmMetricCatalog.identityLabelsOf(metric);
        if (labelFilter.size() > MAX_LABEL_FILTERS) {
            throw new BadRequestException("At most %d series filters are allowed".formatted(MAX_LABEL_FILTERS));
        }
        Map<String, String> normalised = new TreeMap<>();
        for (Map.Entry<String, String> entry : labelFilter.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                throw new BadRequestException("Metric \"%s\" cannot be filtered by \"%s\"".formatted(metric, entry.getKey()));
            }
            String value = entry.getValue();
            if (value == null || value.isBlank() || value.length() > MAX_LABEL_VALUE_LENGTH) {
                throw new BadRequestException("Series filter \"%s\" needs a value of up to %d characters".formatted(
                        entry.getKey(), MAX_LABEL_VALUE_LENGTH));
            }
            normalised.put(entry.getKey(), value);
        }
        return Map.copyOf(normalised);
    }
}
