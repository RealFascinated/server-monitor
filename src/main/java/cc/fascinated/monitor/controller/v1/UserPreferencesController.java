package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.user.UpdateUserPreferenceRequest;
import cc.fascinated.monitor.model.dto.response.user.UserPreferenceResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.UserPreferencesService;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/user/preferences")
public class UserPreferencesController {
    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    public List<UserPreferenceResponse> getPreferences(@AuthenticatedUser UserRow user) {
        return this.userPreferencesService.getPreferences(user.getId());
    }

    @PutMapping("/{key}")
    public UserPreferenceResponse updatePreference(
            @AuthenticatedUser UserRow user,
            @PathVariable String key,
            @Valid @RequestBody UpdateUserPreferenceRequest request
    ) {
        return this.userPreferencesService.updatePreference(user.getId(), key, request.value());
    }
}
