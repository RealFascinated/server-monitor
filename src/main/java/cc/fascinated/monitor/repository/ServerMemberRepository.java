package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerMemberId;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerMemberRepository extends JpaRepository<ServerMemberRow, ServerMemberId> {
    boolean existsByServerIdAndUserId(long serverId, long userId);

    List<ServerMemberRow> findByServerId(long serverId);

    List<ServerMemberRow> findByUserId(long userId);

    @Query("SELECT sm.serverId FROM ServerMemberRow sm WHERE sm.userId = :userId")
    List<Long> findServerIdsByUserId(@Param("userId") long userId);

    void deleteByServerIdAndUserId(long serverId, long userId);
}
