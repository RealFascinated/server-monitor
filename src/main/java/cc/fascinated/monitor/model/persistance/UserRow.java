package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.user.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
public class UserRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public UserRow(String email, UserRole role, String passwordHash, Instant createdAt) {
        this.email = email;
        this.role = role;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}
