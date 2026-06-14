package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.response.settings.SettingResponse;
import cc.fascinated.monitor.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<SettingResponse> getPublicSettings() {
        return this.settingsService.getPublicSettings();
    }
}
