package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.domain.settings.SettingType;
import cc.fascinated.monitor.model.dto.response.settings.SettingResponse;
import cc.fascinated.monitor.model.persistance.SettingRow;
import cc.fascinated.monitor.repository.SettingRepository;
import cc.fascinated.monitor.repository.UserRepository;
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
    private final UserRepository userRepository;

    public SettingsService(SettingRepository settingRepository, UserRepository userRepository) {
        INSTANCE = this;
        this.settingRepository = settingRepository;
        this.userRepository = userRepository;
    }

    public List<SettingResponse> getSettings() {
        return Arrays.stream(Settings.values()).map(this::toResponse).toList();
    }

    public List<SettingResponse> getPublicSettings() {
        return Settings.getPublicSettings().stream().map(this::toPublicResponse).toList();
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

    private SettingResponse toPublicResponse(Settings setting) {
        if (setting == Settings.REGISTRATION_ENABLED && !setting.asBoolean() && this.userRepository.count() == 0) {
            SettingResponse response = toResponse(setting);
            return new SettingResponse(response.key(), response.type(), true, response.lastModified());
        }

        return toResponse(setting);
    }

    @AllArgsConstructor
    @Getter
    enum Settings {
        REGISTRATION_ENABLED("registration_enabled", SettingType.BOOLEAN, true, true);

        private final String key;
        private final SettingType type;
        private final Object defaultValue;
        private final boolean isPublic;

        private static final Map<String, Object> CACHE = new HashMap<>();

        static List<Settings> getPublicSettings() {
            return Arrays.stream(values()).filter(Settings::isPublic).toList();
        }

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
