package cc.fascinated.monitor.model.domain.settings;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import com.fasterxml.jackson.annotation.JsonValue;

public abstract class SettingType {
    public static final SettingType BOOLEAN = new SimpleType("BOOLEAN", Boolean.class);
    public static final SettingType STRING = new SimpleType("STRING", String.class);
    public static final SettingType INTEGER = new SimpleType("INTEGER", Long.class);
    public static final EnumType ENUM = new EnumType();

    private final String name;

    private SettingType(String name) {
        this.name = name;
    }

    @JsonValue
    public String name() {
        return this.name;
    }

    public abstract void validate(Object value);

    private static final class SimpleType extends SettingType {
        private final Class<?> clazz;

        private SimpleType(String name, Class<?> clazz) {
            super(name);
            this.clazz = clazz;
        }

        @Override
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

    public static final class EnumType extends SettingType {
        private EnumType() {
            super("ENUM");
        }

        public <E extends Enum<E> & Preference> SettingType from(Class<E> enumClass) {
            return new BoundEnumType(enumClass);
        }

        @Override
        public void validate(Object value) {
            throw new BadRequestException("Unbound enum setting type");
        }

        private static final class BoundEnumType extends SettingType {
            private final Preference[] constants;

            private <E extends Enum<E> & Preference> BoundEnumType(Class<E> enumClass) {
                super("ENUM");
                this.constants = enumClass.getEnumConstants();
            }

            @Override
            public void validate(Object value) {
                if (value == null) {
                    throw new BadRequestException("Setting value is required");
                }
                if (!(value instanceof String str)) {
                    throw new BadRequestException("Expected string value");
                }
                for (Preference constant : this.constants) {
                    if (constant.getKey().equals(str)) {
                        return;
                    }
                }
                throw new BadRequestException("Invalid value: " + str);
            }
        }
    }
}
