package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerIngestTokenRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerIngestTokenRepository extends JpaRepository<ServerIngestTokenRow, Long> {
    Optional<ServerIngestTokenRow> findByTokenHash(String tokenHash);
}
