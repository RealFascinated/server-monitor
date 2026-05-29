package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.ZfsPoolMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerZfsPoolMetricsRepository extends JpaRepository<ZfsPoolMetricRow, Long> {}
