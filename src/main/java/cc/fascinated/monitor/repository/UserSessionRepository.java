package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.UserSessionRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionRow, Long> {
    Optional<UserSessionRow> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByTokenHash(String tokenHash);
}
