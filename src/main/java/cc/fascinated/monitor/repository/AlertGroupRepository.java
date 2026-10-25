package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertGroupRepository extends JpaRepository<AlertGroupRow, Long> {

    List<AlertGroupRow> findByUserIdOrderByIdAsc(long userId);

    Optional<AlertGroupRow> findByIdAndUserId(long id, long userId);

    long countByUserId(long userId);

    List<AlertGroupRow> findByUserIdAndAutoAddServersTrue(long userId);

    /** Users who lack the default group, e.g. accounts that predate alerting. */
    @Query("SELECT u.id FROM UserRow u WHERE NOT EXISTS (SELECT 1 FROM AlertGroupRow g WHERE g.userId = u.id AND g.defaultGroup = true)")
    List<Long> findUserIdsWithoutDefaultGroup();

    /** Groups of one user that contain a server. */
    @Query("SELECT g FROM AlertGroupRow g WHERE g.userId = :userId AND :serverId MEMBER OF g.serverIds")
    List<AlertGroupRow> findByUserIdAndServerId(@Param("userId") long userId, @Param("serverId") long serverId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM alert_group_servers
            WHERE server_id = :serverId AND group_id IN (SELECT id FROM alert_groups WHERE user_id = :userId)
            """, nativeQuery = true)
    int removeServerFromUserGroups(@Param("userId") long userId, @Param("serverId") long serverId);
}
