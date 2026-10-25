package cc.fascinated.monitor.model.domain.alert;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum AlertSeverity {
    WARNING("Warning", 0xFCD452),
    CRITICAL("Critical", 0xDC2626);

    private final String label;
    private final int color;

    AlertSeverity(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
