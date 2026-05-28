package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerMetricsRepository extends JpaRepository<ServerMetricRow, Long> {}
