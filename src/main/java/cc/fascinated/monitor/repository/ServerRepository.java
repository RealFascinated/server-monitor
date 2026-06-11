package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ServerRepository extends JpaRepository<ServerRow, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ServerRow s
            SET s.status = cc.fascinated.monitor.model.domain.server.ServerStatus.OFFLINE
            WHERE (s.status IS NULL OR s.status = cc.fascinated.monitor.model.domain.server.ServerStatus.ONLINE)
            AND s.lastUpdated IS NOT NULL
            AND s.lastUpdated <= :cutoff
            """)
    int markStaleServersOffline(@Param("cutoff") Instant cutoff);
}
