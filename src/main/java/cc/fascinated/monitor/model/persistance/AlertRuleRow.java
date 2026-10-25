package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "alert_rules")
@NoArgsConstructor
@Getter
@Setter
public class AlertRuleRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long groupId;

    @Nullable
    @Column(name = "name")
    private String name;

    @Column(name = "metric", nullable = false)
    private String metric;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label_filter", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> labelFilter;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false)
    private AlertOperator operator;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Nullable
    @Column(name = "recovery_threshold")
    private Double recoveryThreshold;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Nullable
    @Column(name = "repeat_interval_seconds")
    private Integer repeatIntervalSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public AlertRuleRow(long userId, long groupId, Instant createdAt) {
        this.userId = userId;
        this.groupId = groupId;
        this.createdAt = createdAt;
    }
}
