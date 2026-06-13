package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerFolderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServerFolderRepository extends JpaRepository<ServerFolderRow, Long> {
    List<ServerFolderRow> findByUserIdOrderByPositionAsc(long userId);

    long countByUserId(long userId);

    Optional<ServerFolderRow> findByIdAndUserId(long id, long userId);

    @Query("SELECT f FROM ServerFolderRow f WHERE f.userId = :userId AND LOWER(f.name) = LOWER(:name)")
    Optional<ServerFolderRow> findByUserIdAndNameIgnoreCase(@Param("userId") long userId, @Param("name") String name);

    @Query("SELECT f FROM ServerFolderRow f WHERE f.userId = :userId AND LOWER(f.name) = LOWER(:name) AND f.id <> :folderId")
    Optional<ServerFolderRow> findByUserIdAndNameIgnoreCaseExcludingId(
            @Param("userId") long userId,
            @Param("name") String name,
            @Param("folderId") long folderId
    );
}
