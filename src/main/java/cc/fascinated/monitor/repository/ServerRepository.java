package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.ServerRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<ServerRow, Long> {}
