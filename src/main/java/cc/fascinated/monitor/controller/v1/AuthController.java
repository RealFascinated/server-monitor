package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.auth.ForgotPasswordRequest;
import cc.fascinated.monitor.model.dto.request.auth.LoginRequest;
import cc.fascinated.monitor.model.dto.request.auth.RegisterRequest;
import cc.fascinated.monitor.model.dto.request.auth.ResetPasswordRequest;
import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.dto.response.auth.UserResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.AuthService;
import cc.fascinated.monitor.service.PasswordService;
import cc.fascinated.monitor.service.SessionService;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordService passwordService;
    private final SessionService sessionService;

    public AuthController(
            AuthService authService,
            PasswordService passwordService,
            SessionService sessionService
    ) {
        this.authService = authService;
        this.passwordService = passwordService;
        this.sessionService = sessionService;
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
        this.sessionService.logout(request.getHeader("Authorization"));
    }

    @GetMapping(value = "/@me")
    public UserResponse me(@AuthenticatedUser UserRow user) {
        return UserResponse.from(user);
    }

    @PostMapping(value = "/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        this.passwordService.requestPasswordReset(request.email());
    }

    @PostMapping(value = "/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        this.passwordService.resetPassword(request.token(), request.password());
    }
}
