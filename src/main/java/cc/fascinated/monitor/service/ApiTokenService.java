package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.user.ApiTokenPrincipal;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.request.user.ApiTokenCreateRequest;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenCreatedResponse;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenResponse;
import cc.fascinated.monitor.model.dto.response.user.ApiTokenScopeResponse;
import cc.fascinated.monitor.model.persistance.ApiTokenRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.ApiTokenRepository;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Issues and checks the API tokens MCP clients use. See {@link SessionService} for browser sessions. */
@Slf4j
@Service
public class ApiTokenService {
    public static final String TOKEN_PREFIX = "smk_";
    private static final int TOKEN_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = TOKEN_PREFIX.length() + 8;
    private static final int MAX_TOKENS_PER_USER = 20;
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_EXPIRY_DAYS = 3650;
    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);
    private static final long STALE_RETENTION_DAYS = 30;

    private final ApiTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiTokenService(ApiTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    public List<ApiTokenScopeResponse> scopes() {
        return Arrays.stream(ApiTokenScope.values())
                .map(scope -> new ApiTokenScopeResponse(scope.id(), scope.description()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiTokenResponse> list(UserRow user) {
        return this.tokenRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId()).stream()
                .map(row -> ApiTokenResponse.from(row, parseScopes(row.getScopes())))
                .toList();
    }

    @Transactional
    public ApiTokenCreatedResponse create(UserRow user, ApiTokenCreateRequest request) {
        if (this.tokenRepository.countByUserIdAndRevokedAtIsNull(user.getId()) >= MAX_TOKENS_PER_USER) {
            throw new ConflictException("You can have at most %d API tokens".formatted(MAX_TOKENS_PER_USER));
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("Name must be between 1 and %d characters".formatted(MAX_NAME_LENGTH));
        }
        if (request.scopes() == null || request.scopes().isEmpty()) {
            throw new BadRequestException("Select at least one scope");
        }
        Integer days = request.expiresInDays();
        if (days != null && (days < 1 || days > MAX_EXPIRY_DAYS)) {
            throw new BadRequestException("Expiry must be between 1 and %d days".formatted(MAX_EXPIRY_DAYS));
        }
        Set<ApiTokenScope> scopes = new LinkedHashSet<>();
        for (String id : request.scopes()) {
            scopes.add(ApiTokenScope.fromId(id)
                    .orElseThrow(() -> new BadRequestException("Unknown scope \"%s\"".formatted(id))));
        }
        String token = generateToken();
        Instant now = Instant.now();
        Instant expiresAt = days == null ? null : now.plus(days, ChronoUnit.DAYS);
        ApiTokenRow saved = this.tokenRepository.save(new ApiTokenRow(
                user.getId(),
                name,
                AuthUtils.hash(token),
                token.substring(0, DISPLAY_PREFIX_LENGTH),
                scopes.stream().map(ApiTokenScope::id).collect(Collectors.joining(",")),
                now,
                expiresAt
        ));
        return new ApiTokenCreatedResponse(token, ApiTokenResponse.from(saved, List.copyOf(scopes)));
    }

    @Transactional
    public void revoke(UserRow user, long id) {
        ApiTokenRow row = this.tokenRepository.findByIdAndUserId(id, user.getId())
                .filter(token -> token.getRevokedAt() == null)
                .orElseThrow(() -> new NotFoundException("API token not found"));
        row.setRevokedAt(Instant.now());
    }

    /**
     * Resolves a raw bearer value to the user and scopes it grants.
     *
     * @throws UnauthorizedException if the token is unknown, revoked or expired
     */
    public ApiTokenPrincipal authenticate(String rawToken) {
        if (!rawToken.startsWith(TOKEN_PREFIX)) {
            throw new UnauthorizedException("Invalid or expired API token");
        }
        Instant now = Instant.now();
        ApiTokenRow row = this.tokenRepository.findByTokenHash(AuthUtils.hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt() == null || token.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired API token"));
        UserRow user = this.userRepository.findById(row.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired API token"));
        touch(row, now);
        return new ApiTokenPrincipal(user, Set.copyOf(parseScopes(row.getScopes())));
    }

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void deleteStaleTokens() {
        int deleted = this.tokenRepository.deleteStale(Instant.now().minus(STALE_RETENTION_DAYS, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Deleted {} expired or revoked API token(s)", deleted);
        }
    }

    /** Writes last-used at most once per {@link #LAST_USED_RESOLUTION}, so a busy client doesn't write on every call. */
    private void touch(ApiTokenRow row, Instant now) {
        Instant previous = row.getLastUsedAt();
        if (previous != null && Duration.between(previous, now).compareTo(LAST_USED_RESOLUTION) < 0) {
            return;
        }
        this.tokenRepository.touch(row.getId(), now);
    }

    private static List<ApiTokenScope> parseScopes(String stored) {
        EnumSet<ApiTokenScope> scopes = EnumSet.noneOf(ApiTokenScope.class);
        for (String id : stored.split(",")) {
            ApiTokenScope.fromId(id.trim()).ifPresent(scopes::add);
        }
        return List.copyOf(scopes);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
