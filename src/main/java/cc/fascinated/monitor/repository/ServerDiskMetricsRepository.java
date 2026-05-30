package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.DiskMetricRow;
import cc.fascinated.monitor.model.persistance.metric.MetricRowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerDiskMetricsRepository extends JpaRepository<DiskMetricRow, MetricRowId> {}
