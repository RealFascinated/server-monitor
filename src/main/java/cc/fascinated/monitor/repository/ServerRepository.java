package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.persistance.ServerRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ServerRepository extends JpaRepository<ServerRow, Long> {

    @Query("""
            SELECT s FROM ServerRow s
            WHERE (s.status IS NULL OR s.status = :onlineStatus)
            AND s.lastHeartbeat IS NOT NULL
            AND s.lastHeartbeat <= :cutoff
            """)
    List<ServerRow> findStaleServers(
            @Param("cutoff") Instant cutoff,
            @Param("onlineStatus") ServerStatus onlineStatus
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ServerRow s
            SET s.status = :offlineStatus
            WHERE (s.status IS NULL OR s.status = :onlineStatus)
            AND s.lastHeartbeat IS NOT NULL
            AND s.lastHeartbeat <= :cutoff
            """)
    int markStaleServersOffline(
            @Param("cutoff") Instant cutoff,
            @Param("offlineStatus") ServerStatus offlineStatus,
            @Param("onlineStatus") ServerStatus onlineStatus
    );
}
