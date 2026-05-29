package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.metric.DockerContainerMetricRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerDockerContainerMetricsRepository extends JpaRepository<DockerContainerMetricRow, Long> {}
