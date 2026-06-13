package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.settings.UpdateSettingRequest;
import cc.fascinated.monitor.model.dto.response.settings.SettingResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.SettingsService;
import cc.fascinated.monitor.web.auth.AuthenticatedAdmin;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/admin/settings")
public class AdminSettingsController {
    private final SettingsService settingsService;

    public AdminSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<SettingResponse> getSettings(@AuthenticatedAdmin UserRow admin) {
        return this.settingsService.getSettings();
    }

    @PutMapping(value = "/{key}")
    public SettingResponse updateSetting(
            @AuthenticatedAdmin UserRow admin,
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request
    ) {
        return this.settingsService.updateSetting(key, request.value());
    }
}
