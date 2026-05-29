package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.IngestTokenRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerIngestTokenRepository extends JpaRepository<IngestTokenRow, Long> {
    Optional<IngestTokenRow> findByTokenHash(String tokenHash);
}
