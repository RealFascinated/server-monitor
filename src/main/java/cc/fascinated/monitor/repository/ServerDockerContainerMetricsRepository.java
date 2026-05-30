package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.DockerContainerMetricRow;
import cc.fascinated.monitor.model.persistance.metric.MetricRowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerDockerContainerMetricsRepository extends JpaRepository<DockerContainerMetricRow, MetricRowId> {}
