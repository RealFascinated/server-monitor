package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRuleRow, Long> {

    List<AlertRuleRow> findByUserIdOrderByIdDesc(long userId);

    List<AlertRuleRow> findByGroupIdOrderByIdDesc(long groupId);

    List<AlertRuleRow> findByGroupId(long groupId);

    long countByGroupId(long groupId);

    Optional<AlertRuleRow> findByIdAndUserId(long id, long userId);

    long countByUserId(long userId);

    long countByDestinationId(long destinationId);

    List<AlertRuleRow> findByDestinationId(long destinationId);

    /** Rules a server inherits from the enabled groups it is in, for owners who still have access to it. */
    @Query("""
            SELECT r FROM AlertRuleRow r
            WHERE r.groupId IN (SELECT g.id FROM AlertGroupRow g WHERE :serverId MEMBER OF g.serverIds AND g.enabled = true)
            AND EXISTS (SELECT 1 FROM ServerMemberRow m WHERE m.serverId = :serverId AND m.userId = r.userId)
            """)
    List<AlertRuleRow> findApplicableToServer(@Param("serverId") long serverId);

    @Query("SELECT r.id FROM AlertRuleRow r WHERE r.groupId IN :groupIds")
    List<Long> findIdsByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT r.id FROM AlertRuleRow r WHERE r.destinationId = :destinationId")
    List<Long> findIdsByDestinationId(@Param("destinationId") long destinationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE AlertRuleRow r SET r.destinationId = :destinationId WHERE r.userId = :userId AND r.destinationId IS NULL")
    int attachDestination(@Param("userId") long userId, @Param("destinationId") long destinationId);

    @Query("SELECT r FROM AlertRuleRow r WHERE r.id IN :ids")
    List<AlertRuleRow> findAllByIds(@Param("ids") Collection<Long> ids);
}
