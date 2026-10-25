package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.user.ApiTokenCreateRequest;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenCreatedResponse;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenResponse;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenScopeResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.ApiTokenService;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Token management. Uses session auth only, so an API token can never mint or revoke tokens. */
@RestController
@RequestMapping(value = "/v1/user/api-tokens")
public class ApiTokenController {
    private final ApiTokenService apiTokenService;

    public ApiTokenController(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @GetMapping
    public List<ApiTokenResponse> list(@AuthenticatedUser UserRow user) {
        return this.apiTokenService.list(user);
    }

    @GetMapping("/scopes")
    public List<ApiTokenScopeResponse> scopes(@AuthenticatedUser UserRow user) {
        return this.apiTokenService.scopes();
    }

    @PostMapping
    public ApiTokenCreatedResponse create(@AuthenticatedUser UserRow user,
                                          @RequestBody ApiTokenCreateRequest request) {
        return this.apiTokenService.create(user, request);
    }

    @DeleteMapping("/{id}")
    public void revoke(@AuthenticatedUser UserRow user, @PathVariable long id) {
        this.apiTokenService.revoke(user, id);
    }
}
