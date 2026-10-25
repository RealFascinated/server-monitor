package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** A set of servers that share the alert rules attached to the group. */
@Entity
@Table(name = "alert_groups")
@NoArgsConstructor
@Getter
@Setter
public class AlertGroupRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    /** New servers the owner gains access to join this group automatically. */
    @Column(name = "auto_add_servers", nullable = false)
    private boolean autoAddServers;

    /** Disabled groups keep their rules and servers but nothing is evaluated for them. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** The group every user starts with; it cannot be deleted. */
    @Column(name = "is_default", nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean defaultGroup;

    @ElementCollection
    @CollectionTable(name = "alert_group_servers", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "server_id", nullable = false)
    private Set<Long> serverIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public AlertGroupRow(long userId, String name, boolean autoAddServers, Instant createdAt) {
        this.userId = userId;
        this.name = name;
        this.autoAddServers = autoAddServers;
        this.createdAt = createdAt;
    }

    public static AlertGroupRow defaultGroup(long userId, String name, Instant createdAt) {
        AlertGroupRow row = new AlertGroupRow(userId, name, true, createdAt);
        row.defaultGroup = true;
        row.enabled = false;
        return row;
    }
}
