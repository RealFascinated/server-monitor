package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.ForbiddenException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.user.UserRole;
import cc.fascinated.monitor.model.dto.request.auth.LoginRequest;
import cc.fascinated.monitor.model.dto.request.auth.RegisterRequest;
import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.UserUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            SessionService sessionService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        if (!SettingsService.Settings.REGISTRATION_ENABLED.asBoolean() && this.userRepository.count() > 0) {
            throw new ForbiddenException("Registration is disabled");
        }

        String email = UserUtils.normalizeEmail(request.email());
        if (this.userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        Instant now = Instant.now();
        UserRow user = this.userRepository.save(new UserRow(
                email,
                this.userRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER,
                encodePassword(request.password()),
                now
        ));
        return this.sessionService.createSession(user);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String email = UserUtils.normalizeEmail(request.email());
        UserRow user = this.userRepository.findByEmailIgnoreCase(email)
                .filter(u -> matchesPassword(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return this.sessionService.createSession(user);
    }

    private String encodePassword(String rawPassword) {
        return this.passwordEncoder.encode(AuthUtils.hash(rawPassword));
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        return this.passwordEncoder.matches(AuthUtils.hash(rawPassword), storedHash);
    }
}
