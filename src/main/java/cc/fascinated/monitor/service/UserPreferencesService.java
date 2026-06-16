package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.domain.metric.MetricRefreshInterval;
import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.domain.settings.SettingType;
import cc.fascinated.monitor.model.domain.user.ThemePreference;
import cc.fascinated.monitor.model.dto.response.user.UserPreferenceResponse;
import cc.fascinated.monitor.model.persistance.UserPreferenceRow;
import cc.fascinated.monitor.repository.UserPreferenceRepository;
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
public class UserPreferencesService {
    public static UserPreferencesService INSTANCE;

    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferencesService(UserPreferenceRepository userPreferenceRepository) {
        INSTANCE = this;
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public List<UserPreferenceResponse> getPreferences(Long userId) {
        return Arrays.stream(Preferences.values()).map(p -> toResponse(p, userId)).toList();
    }

    @Transactional
    public UserPreferenceResponse updatePreference(Long userId, String key, Object value) {
        Preferences preference = Preferences.fromKey(key);
        preference.getType().validate(value);
        preference.setValue(userId, value);
        return toResponse(preference, userId);
    }

    private UserPreferenceResponse toResponse(Preferences preference, Long userId) {
        Optional<UserPreferenceRow> row = this.userPreferenceRepository.findById(new UserPreferenceRow.Id(userId, preference.getKey()));
        Object value = row.map(UserPreferenceRow::getValue).orElse(preference.getDefaultValue());
        Instant lastModified = row.map(UserPreferenceRow::getLastModified).orElse(null);
        return new UserPreferenceResponse(preference.getKey(), preference.getType(), value, lastModified);
    }

    @AllArgsConstructor
    @Getter
    public enum Preferences {
        THEME("theme", SettingType.ENUM.from(ThemePreference.class), "system"),
        METRIC_DEFAULT_RANGE("metric_default_range", SettingType.ENUM.from(MetricTimeRange.class), "7d"),
        METRIC_REFRESH_INTERVAL("metric_refresh_interval", SettingType.ENUM.from(MetricRefreshInterval.class), "10s"),
        SIDEBAR_DETAILED_MODE("sidebar_detailed_mode", SettingType.BOOLEAN, false),
        SIDEBAR_COLUMNS_CPU("sidebar_columns_cpu", SettingType.BOOLEAN, true),
        SIDEBAR_COLUMNS_RAM("sidebar_columns_ram", SettingType.BOOLEAN, true),
        SIDEBAR_WIDTH("sidebar_width", SettingType.INTEGER, 224L);

        private final String key;
        private final SettingType type;
        private final Object defaultValue;

        private static final Map<Long, Map<String, Object>> CACHE = new HashMap<>();

        public static Preferences fromKey(String key) {
            for (Preferences pref : values()) {
                if (pref.key.equals(key)) {
                    return pref;
                }
            }
            throw new NotFoundException("Unknown preference: " + key);
        }

        public boolean asBoolean(Long userId) {
            return (boolean) getValue(userId);
        }

        Object getValue(Long userId) {
            Map<String, Object> userCache = CACHE.get(userId);
            if (userCache != null) {
                Object cached = userCache.get(this.key);
                if (cached != null) {
                    return cached;
                }
            }
            Object value = INSTANCE.getUserPreferenceRepository()
                    .findById(new UserPreferenceRow.Id(userId, this.key))
                    .map(UserPreferenceRow::getValue)
                    .orElse(this.defaultValue);
            CACHE.computeIfAbsent(userId, k -> new HashMap<>()).put(this.key, value);
            return value;
        }

        void setValue(Long userId, Object value) {
            INSTANCE.getUserPreferenceRepository().save(new UserPreferenceRow(userId, this.key, value));
            CACHE.computeIfAbsent(userId, k -> new HashMap<>()).put(this.key, value);
        }
    }
}
