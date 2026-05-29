package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.InterfaceMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerInterfaceMetricsRepository extends JpaRepository<InterfaceMetricRow, Long> {}
