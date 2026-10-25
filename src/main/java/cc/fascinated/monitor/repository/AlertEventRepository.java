package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.AlertEventRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertEventRepository extends JpaRepository<AlertEventRow, Long>, JpaSpecificationExecutor<AlertEventRow> {

    Optional<AlertEventRow> findFirstByRuleIdAndServerIdAndSeriesKeyAndResolvedAtIsNull(
            long ruleId, long serverId, String seriesKey);

    List<AlertEventRow> findByResolvedAtIsNull();

    @Query("""
            SELECT new cc.fascinated.monitor.repository.AlertRuleEventStat(
                e.ruleId,
                COUNT(CASE WHEN e.resolvedAt IS NULL AND e.silencedAt IS NULL THEN 1 END),
                COUNT(CASE WHEN e.resolvedAt IS NULL AND e.silencedAt IS NOT NULL THEN 1 END),
                MAX(e.startedAt))
            FROM AlertEventRow e
            WHERE e.ruleId IN :ruleIds
            GROUP BY e.ruleId
            """)
    List<AlertRuleEventStat> statsByRuleIds(@Param("ruleIds") Collection<Long> ruleIds);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE AlertEventRow e SET e.resolvedAt = :resolvedAt, e.resolveReason = :reason
            WHERE e.ruleId IN :ruleIds AND e.resolvedAt IS NULL
            """)
    int closeOpenEvents(
            @Param("ruleIds") Collection<Long> ruleIds,
            @Param("resolvedAt") Instant resolvedAt,
            @Param("reason") String reason
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE AlertEventRow e SET e.resolvedAt = :resolvedAt, e.resolveReason = :reason
            WHERE e.ruleId IN :ruleIds AND e.serverId = :serverId AND e.resolvedAt IS NULL
            """)
    int closeOpenEventsForServer(
            @Param("ruleIds") Collection<Long> ruleIds,
            @Param("serverId") long serverId,
            @Param("resolvedAt") Instant resolvedAt,
            @Param("reason") String reason
    );

    @Query("SELECT COUNT(e) FROM AlertEventRow e WHERE e.ruleId IN :ruleIds AND e.resolvedAt IS NULL")
    long countOpenByRuleIds(@Param("ruleIds") Collection<Long> ruleIds);

    /** Targeted updates so a concurrent resolve by the dispatcher is never overwritten with a stale row. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AlertEventRow e SET e.silencedAt = :at, e.silencedValue = :value
            WHERE e.id = :id AND e.resolvedAt IS NULL
            """)
    int silenceOpenEvent(@Param("id") long id, @Param("at") Instant at, @Param("value") double value);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AlertEventRow e SET e.silencedAt = NULL, e.silencedValue = NULL
            WHERE e.id = :id AND e.resolvedAt IS NULL
            """)
    int unsilenceOpenEvent(@Param("id") long id);
}
