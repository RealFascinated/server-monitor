package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.ServerZfsArcMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerZfsArcMetricsRepository extends JpaRepository<ServerZfsArcMetricRow, Long> {}
