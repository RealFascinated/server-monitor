package cc.fascinated.monitor.model.domain.settings;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SettingType {
    BOOLEAN(Boolean.class),
    STRING(String.class),
    INTEGER(Long.class);

    private final Class<?> clazz;

    public void validate(Object value) {
        if (value == null) {
            throw new BadRequestException("Setting value is required");
        }
        if (this.clazz.isInstance(value)) {
            return;
        }
        if (this.clazz == Long.class && value instanceof Integer) {
            return;
        }
        throw new BadRequestException("Expected " + this.clazz.getSimpleName() + " value");
    }
}
