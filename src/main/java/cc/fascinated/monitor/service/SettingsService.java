package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.domain.settings.SettingType;
import cc.fascinated.monitor.model.dto.response.settings.SettingResponse;
import cc.fascinated.monitor.model.persistance.SettingRow;
import cc.fascinated.monitor.repository.SettingRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Getter
public class SettingsService {
    public static SettingsService INSTANCE;

    private final SettingRepository settingRepository;

    public SettingsService(SettingRepository settingRepository) {
        INSTANCE = this;
        this.settingRepository = settingRepository;
    }

    public List<SettingResponse> getSettings() {
        return Arrays.stream(Settings.values()).map(this::toResponse).toList();
    }

    @Transactional
    public SettingResponse updateSetting(String key, Object value) {
        Settings setting = Settings.fromKey(key);
        setting.getType().validate(value);
        setting.setValue(value);
        return toResponse(setting);
    }

    private SettingResponse toResponse(Settings setting) {
        Optional<SettingRow> row = this.settingRepository.findById(setting.getKey());
        Object value = row.map(SettingRow::getValue).orElse(setting.getDefaultValue());
        Instant lastModified = row.map(SettingRow::getLastModified).orElse(null);
        return new SettingResponse(setting.getKey(), setting.getType(), value, lastModified);
    }

    @AllArgsConstructor
    @Getter
    enum Settings {
        REGISTRATION_ENABLED("registration_enabled", SettingType.BOOLEAN, true);

        private final String key;
        private final SettingType type;
        private final Object defaultValue;

        private static final Map<String, Object> CACHE = new HashMap<>();

        static Settings fromKey(String key) {
            for (Settings setting : values()) {
                if (setting.key.equals(key)) {
                    return setting;
                }
            }
            throw new NotFoundException("Unknown setting: " + key);
        }

        public boolean asBoolean() {
            return (boolean) getValue();
        }

        Object getValue() {
            Object cached = CACHE.get(this.key);
            if (cached != null) {
                return cached;
            }

            Object value = INSTANCE.getSettingRepository().findById(this.key)
                    .map(SettingRow::getValue)
                    .orElse(this.defaultValue);
            CACHE.put(this.key, value);
            return value;
        }

        void setValue(Object value) {
            INSTANCE.getSettingRepository().save(new SettingRow(this.key, value));
            CACHE.put(this.key, value);
        }
    }
}
