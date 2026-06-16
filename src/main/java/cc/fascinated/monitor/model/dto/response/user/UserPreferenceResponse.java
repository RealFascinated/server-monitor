package cc.fascinated.monitor.model.dto.response.user;

import cc.fascinated.monitor.model.domain.settings.SettingType;

import java.time.Instant;

public record UserPreferenceResponse(
        String key,
        SettingType type,
        Object value,
        Instant lastModified
) {}
