package cc.fascinated.monitor.repository;

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
            LEFT JOIN FETCH s.inventory
            WHERE s.ownerId = :ownerId
            ORDER BY s.createdAt DESC
            """)
    List<ServerRow> findByOwnerIdWithInventory(@Param("ownerId") long ownerId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ServerRow s
            SET s.status = cc.fascinated.monitor.model.domain.server.ServerStatus.OFFLINE
            WHERE (s.status IS NULL OR s.status = cc.fascinated.monitor.model.domain.server.ServerStatus.ONLINE)
            AND (
                (s.lastUpdated IS NOT NULL AND s.lastUpdated <= :cutoff)
                OR (s.lastUpdated IS NULL AND s.createdAt <= :cutoff)
            )
            """)
    int markStaleServersOffline(@Param("cutoff") Instant cutoff);
}
