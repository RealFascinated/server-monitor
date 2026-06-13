package cc.fascinated.monitor.model.dto.response.settings;

import cc.fascinated.monitor.model.domain.settings.SettingType;

import java.time.Instant;

public record SettingResponse(
        String key,
        SettingType type,
        Object value,
        Instant lastModified
) {}
