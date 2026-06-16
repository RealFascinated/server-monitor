package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.persistance.PasswordResetTokenRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.PasswordResetTokenRepository;
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
public class PasswordService {
    private static final int RESET_TOKEN_BYTES = 32;
    private static final long RESET_TOKEN_DURATION_HOURS = 1;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @Transactional
    public void changePassword(UserRow user, String currentPassword, String newPassword, String currentSessionToken) {
        if (!matchesPassword(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(encodePassword(newPassword));
        this.userRepository.save(user);
        this.userSessionRepository.deleteByUserIdAndTokenHashNot(
                user.getId(),
                AuthUtils.hash(currentSessionToken)
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
        this.userRepository.findByEmailIgnoreCase(UserUtils.normalizeEmail(email)).ifPresent(user -> {
            this.passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = generateResetToken();
            Instant now = Instant.now();
            this.passwordResetTokenRepository.save(new PasswordResetTokenRow(
                    user.getId(),
                    AuthUtils.hash(token),
                    now.plus(RESET_TOKEN_DURATION_HOURS, ChronoUnit.HOURS),
                    now
            ));
            this.mailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenRow resetToken = this.passwordResetTokenRepository
                .findByTokenHashAndExpiresAtAfter(AuthUtils.hash(token), Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        UserRow user = this.userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        user.setPasswordHash(encodePassword(newPassword));
        this.userRepository.save(user);
        this.passwordResetTokenRepository.delete(resetToken);
        this.userSessionRepository.deleteByUserId(user.getId());
    }

    private String generateResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
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
