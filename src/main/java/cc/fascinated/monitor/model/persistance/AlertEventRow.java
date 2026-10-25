package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "alert_events")
@NoArgsConstructor
@Getter
@Setter
public class AlertEventRow {
    public static final String REASON_RECOVERED = "RECOVERED";
    public static final String REASON_SERIES_GONE = "SERIES_GONE";
    public static final String REASON_RULE_CHANGED = "RULE_CHANGED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long ruleId;

    @Column(name = "server_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long serverId;

    @Column(name = "series_key", nullable = false)
    @Setter(AccessLevel.NONE)
    private String seriesKey;

    @Column(name = "started_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant startedAt;

    @Nullable
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Nullable
    @Column(name = "resolve_reason")
    private String resolveReason;

    @Column(name = "peak_value", nullable = false)
    private double peakValue;

    @Nullable
    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @Nullable
    @Column(name = "notify_error")
    private String notifyError;

    @Nullable
    @Column(name = "silenced_at")
    private Instant silencedAt;

    @Nullable
    @Column(name = "silenced_value")
    private Double silencedValue;

    public AlertEventRow(long ruleId, long serverId, String seriesKey, Instant startedAt, double peakValue) {
        this.ruleId = ruleId;
        this.serverId = serverId;
        this.seriesKey = seriesKey;
        this.startedAt = startedAt;
        this.peakValue = peakValue;
    }
}
