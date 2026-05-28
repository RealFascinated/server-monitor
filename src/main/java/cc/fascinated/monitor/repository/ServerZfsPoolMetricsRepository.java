package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.ServerZfsPoolMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerZfsPoolMetricsRepository extends JpaRepository<ServerZfsPoolMetricRow, Long> {}
