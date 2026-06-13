package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "settings")
@NoArgsConstructor
@Getter
public class SettingRow {
    @Id
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private Object value;

    @Column(name = "last_modified", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant lastModified;

    public SettingRow(String key, Object value) {
        this.key = key;
        this.value = value;
        this.lastModified = Instant.now();
    }
}
