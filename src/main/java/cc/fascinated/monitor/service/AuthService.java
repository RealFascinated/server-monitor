package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.ForbiddenException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.user.UserRole;
import cc.fascinated.monitor.model.dto.request.auth.LoginRequest;
import cc.fascinated.monitor.model.dto.request.auth.RegisterRequest;
import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.model.persistance.UserSessionRow;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.repository.UserSessionRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.UserUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {
    private static final int SESSION_TOKEN_BYTES = 32;
    private static final long SESSION_DURATION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, UserSessionRepository userSessionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
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
        return createSession(user);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String email = UserUtils.normalizeEmail(request.email());
        UserRow user = this.userRepository.findByEmailIgnoreCase(email)
                .filter(u -> matchesPassword(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return createSession(user);
    }

    public UserRow authenticate(String authorizationHeader) {
        return authenticateToken(AuthUtils.extractBearerValue(authorizationHeader));
    }

    public UserRow authenticateToken(String token) {
        UserSessionRow session = this.userSessionRepository
                .findByTokenHashAndExpiresAtAfter(AuthUtils.hash(token), Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
        return this.userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = AuthUtils.extractBearerValue(authorizationHeader);
        this.userSessionRepository.deleteByTokenHash(AuthUtils.hash(token));
    }

    private AuthTokenResponse createSession(UserRow user) {
        String token = generateSessionToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_DURATION_DAYS, ChronoUnit.DAYS);
        this.userSessionRepository.save(new UserSessionRow(
                user.getId(),
                AuthUtils.hash(token),
                expiresAt,
                now
        ));
        return new AuthTokenResponse(token, expiresAt);
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encodePassword(String rawPassword) {
        return this.passwordEncoder.encode(AuthUtils.hash(rawPassword));
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        return this.passwordEncoder.matches(AuthUtils.hash(rawPassword), storedHash);
    }
}
