package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.PasswordResetTokenRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenRow, Long> {
    Optional<PasswordResetTokenRow> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByUserId(long userId);
}
