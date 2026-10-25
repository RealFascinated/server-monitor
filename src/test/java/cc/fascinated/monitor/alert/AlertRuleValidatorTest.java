package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertRuleValidatorTest {
    private final AlertRuleValidator validator = new AlertRuleValidator(new MonitorAlertProperties());

    @Test
    void acceptsAValidWindowedRule() {
        var definition = this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.MORE_THAN, 90, 80.0, 300);

        assertThat(definition.spec().windowSeconds()).isEqualTo(300);
        assertThat(definition.spec().recoveryThreshold()).isEqualTo(80.0);
    }

    @Test
    void rejectsUnknownMetricsAndLabels() {
        assertThatThrownBy(() -> this.validator.validate("nope", null, AlertOperator.MORE_THAN, 1, null, 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", Map.of("disk", "/"),
                AlertOperator.MORE_THAN, 1, null, 0)).isInstanceOf(BadRequestException.class);
        assertThat(this.validator.validate("monitor_disk_usage_percent", Map.of("disk", "/"),
                AlertOperator.MORE_THAN, 80, null, 0).labelFilter()).containsEntry("disk", "/");
    }

    @Test
    void rejectsBadNumbersAndWindows() {
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.MORE_THAN,
                Double.NaN, null, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.MORE_THAN,
                90, 95.0, 0)).as("recovery above threshold").isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.LESS_THAN,
                10, 5.0, 0)).as("recovery below threshold").isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.MORE_THAN,
                90, null, 5)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> this.validator.validate("monitor_host_cpu_usage", null, AlertOperator.MORE_THAN,
                90, null, 90_000)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void serverUpRulesMustBeOfflineShaped() {
        var definition = this.validator.validate(ServerStatusSeries.UP.metricName(), null, AlertOperator.LESS_THAN, 1, 1.0, 600);

        assertThat(definition.spec().windowSeconds()).isZero();
        assertThat(definition.spec().recoveryThreshold()).isNull();
        assertThatThrownBy(() -> this.validator.validate(ServerStatusSeries.UP.metricName(), null, AlertOperator.MORE_THAN, 1, null, 0))
                .isInstanceOf(BadRequestException.class);
    }
}
