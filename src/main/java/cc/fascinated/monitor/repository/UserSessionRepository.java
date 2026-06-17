package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.UserSessionRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionRow, Long> {
    Optional<UserSessionRow> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<UserSessionRow> findByIdAndUserId(long id, long userId);

    List<UserSessionRow> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(long userId, Instant now);

    long countByExpiresAtAfter(Instant now);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserId(long userId);

    void deleteByUserIdAndTokenHashNot(long userId, String tokenHash);
}
