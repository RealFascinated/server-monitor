package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ApiTokenRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiTokenRow, Long> {
    Optional<ApiTokenRow> findByTokenHash(String tokenHash);

    Optional<ApiTokenRow> findByIdAndUserId(long id, long userId);

    List<ApiTokenRow> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(long userId);

    long countByUserIdAndRevokedAtIsNull(long userId);

    /** Records a use without loading the row, so authenticating never contends on the entity. */
    @Transactional
    @Modifying
    @Query("update ApiTokenRow t set t.lastUsedAt = :at where t.id = :id")
    void touch(long id, Instant at);

    /** Removes tokens that expired or were revoked before {@code cutoff}. */
    @Modifying
    @Query("delete from ApiTokenRow t where (t.expiresAt is not null and t.expiresAt < :cutoff) "
            + "or (t.revokedAt is not null and t.revokedAt < :cutoff)")
    int deleteStale(Instant cutoff);
}
