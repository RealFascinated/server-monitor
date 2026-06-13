package cc.fascinated.monitor.repository;

import cc.fascinated.monitor.model.persistance.SettingRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SettingRow, String> { }
