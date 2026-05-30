package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.UserRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserRow, Long> {
    Optional<UserRow> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
