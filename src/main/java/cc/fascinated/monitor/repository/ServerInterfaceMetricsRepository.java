package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.ServerInterfaceMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerInterfaceMetricsRepository extends JpaRepository<ServerInterfaceMetricRow, Long> {}
