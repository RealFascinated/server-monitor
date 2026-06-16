package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.UserPreferenceRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceRow, UserPreferenceRow.Id> {
    List<UserPreferenceRow> findByIdUserId(Long userId);
}
