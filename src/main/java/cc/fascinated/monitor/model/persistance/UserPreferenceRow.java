package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_preferences")
@NoArgsConstructor
@Getter
public class UserPreferenceRow {
    @EmbeddedId
    private Id id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private Object value;

    @Column(name = "last_modified", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant lastModified;

    public UserPreferenceRow(Long userId, String key, Object value) {
        this.id = new Id(userId, key);
        this.value = value;
        this.lastModified = Instant.now();
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "key", nullable = false)
        private String key;
    }
}
