package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerInviteRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ServerInviteRepository extends JpaRepository<ServerInviteRow, Long> {
    boolean existsByServerIdAndEmailIgnoreCase(long serverId, String email);

    List<ServerInviteRow> findByServerId(long serverId);

    List<ServerInviteRow> findByEmailIgnoreCaseAndExpiresAtAfter(String email, Instant now);

    Optional<ServerInviteRow> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
}
