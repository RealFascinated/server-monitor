package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_folders")
@NoArgsConstructor
@Getter
@Setter
public class ServerFolderRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public ServerFolderRow(String name, long userId) {
        this.name = name;
        this.userId = userId;
    }
}
