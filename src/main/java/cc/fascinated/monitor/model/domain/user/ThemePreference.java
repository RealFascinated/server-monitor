package cc.fascinated.monitor.model.domain.user;

import cc.fascinated.monitor.model.domain.settings.Preference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ThemePreference implements Preference {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    private final String key;
}
