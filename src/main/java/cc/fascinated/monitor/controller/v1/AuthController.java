package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.auth.LoginRequest;
import cc.fascinated.monitor.model.dto.request.auth.RegisterRequest;
import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.dto.response.auth.UserResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.AuthService;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/register")
    public AuthTokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return this.authService.register(request);
    }

    @PostMapping(value = "/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return this.authService.login(request);
    }

    @PostMapping(value = "/logout")
    public void logout(HttpServletRequest request) {
        this.authService.logout(request.getHeader("Authorization"));
    }

    @GetMapping(value = "/@me")
    public UserResponse me(@AuthenticatedUser UserRow user) {
        return UserResponse.from(user);
    }
}
