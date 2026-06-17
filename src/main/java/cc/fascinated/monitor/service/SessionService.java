package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.ClientInfo;
import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.dto.response.user.UserSessionResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.model.persistance.UserSessionRow;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.repository.UserSessionRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.UserAgentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class SessionService {
    private static final int SESSION_TOKEN_BYTES = 32;
    private static final long SESSION_DURATION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final EncryptionService encryptionService;
    private final MaxMindService maxMindService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            EncryptionService encryptionService,
            MaxMindService maxMindService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.encryptionService = encryptionService;
        this.maxMindService = maxMindService;
    }
    
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredSessions() {
        long deleted = this.userSessionRepository.deleteByExpiresAtLessThanEqual(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired session(s)", deleted);
        }
    }

    @Transactional
    public AuthTokenResponse createSession(UserRow user, ClientInfo clientInfo) {
        String token = generateSessionToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_DURATION_DAYS, ChronoUnit.DAYS);
        this.userSessionRepository.save(new UserSessionRow(
                user.getId(),
                AuthUtils.hash(token),
                expiresAt,
                now,
                clientInfo.ipEncrypted(),
                clientInfo.userAgent()
        ));
        return new AuthTokenResponse(token, expiresAt);
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

    public List<UserSessionResponse> listSessions(UserRow user, String currentSessionToken) {
        String currentTokenHash = AuthUtils.hash(currentSessionToken);
        Instant now = Instant.now();
        return this.userSessionRepository
                .findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), now)
                .stream()
                .map(session -> toResponse(
                        session,
                        session.getTokenHash().equals(currentTokenHash)
                ))
                .toList();
    }

    private UserSessionResponse toResponse(UserSessionRow session, boolean current) {
        return new UserSessionResponse(
                session.getId(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                current,
                UserAgentUtils.formatDeviceLabel(session.getUserAgent()),
                resolveLocationLabel(session.getIpEncrypted())
        );
    }

    private String resolveLocationLabel(String ipEncrypted) {
        if (ipEncrypted == null || ipEncrypted.isBlank()) {
            return null;
        }
        return this.maxMindService.formatLocation(this.encryptionService.decrypt(ipEncrypted)).orElse(null);
    }

    @Transactional
    public void revokeSession(UserRow user, long sessionId, String currentSessionToken) {
        UserSessionRow session = this.userSessionRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getTokenHash().equals(AuthUtils.hash(currentSessionToken))) {
            throw new BadRequestException("Cannot revoke the current session");
        }

        this.userSessionRepository.delete(session);
    }

    @Transactional
    public void revokeOtherSessions(UserRow user, String currentSessionToken) {
        this.userSessionRepository.deleteByUserIdAndTokenHashNot(
                user.getId(),
                AuthUtils.hash(currentSessionToken)
        );
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
