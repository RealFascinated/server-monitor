package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.alert.AlertDestination;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "alert_destinations")
@NoArgsConstructor
@Getter
@Setter
public class AlertDestinationRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Setter(AccessLevel.NONE)
    private AlertDestination type;

    @Column(name = "config_encrypted", nullable = false)
    private String configEncrypted;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public AlertDestinationRow(long userId, String name, AlertDestination type, String configEncrypted, Instant createdAt) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.configEncrypted = configEncrypted;
        this.createdAt = createdAt;
    }
}
