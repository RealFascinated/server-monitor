package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerDiskMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerDiskMetricsRepository extends JpaRepository<ServerDiskMetricRow, Long> {}
