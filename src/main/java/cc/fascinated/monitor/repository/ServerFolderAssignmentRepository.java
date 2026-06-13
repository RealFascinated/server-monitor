package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerFolderAssignmentRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerFolderAssignmentRepository extends JpaRepository<ServerFolderAssignmentRow, Long> {
    @Modifying
    @Query("""
            DELETE FROM ServerFolderAssignmentRow a
            WHERE a.serverId = :serverId
              AND a.folderId IN (SELECT f.id FROM ServerFolderRow f WHERE f.userId = :userId)
            """)
    void deleteByServerIdAndUserId(@Param("serverId") long serverId, @Param("userId") long userId);

    @Query("""
            SELECT a.serverId, f.name
            FROM ServerFolderAssignmentRow a
            JOIN ServerFolderRow f ON f.id = a.folderId
            WHERE f.userId = :userId AND a.serverId IN :serverIds
            """)
    List<Object[]> findFolderNamesByUserIdAndServerIds(
            @Param("userId") long userId,
            @Param("serverIds") List<Long> serverIds
    );
}
