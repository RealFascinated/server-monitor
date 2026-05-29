package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.DiskMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerDiskMetricsRepository extends JpaRepository<DiskMetricRow, Long> {}
