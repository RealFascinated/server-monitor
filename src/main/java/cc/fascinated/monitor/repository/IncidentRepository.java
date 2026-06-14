package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.IncidentRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentRow, Long> {

    long countByServerId(long serverId);

    List<IncidentRow> findByServerIdOrderByStartedAtDesc(long serverId, Pageable pageable);

    Optional<IncidentRow> findByServerIdAndResolvedAtIsNull(long serverId);
}
