package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.AlertDestinationRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertDestinationRepository extends JpaRepository<AlertDestinationRow, Long> {

    List<AlertDestinationRow> findByUserIdOrderByIdAsc(long userId);

    Optional<AlertDestinationRow> findByIdAndUserId(long id, long userId);

    long countByUserId(long userId);
}
