package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_folder_assignments")
@NoArgsConstructor
@Getter
@Setter
public class ServerFolderAssignmentRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    public ServerFolderAssignmentRow(long folderId, long serverId) {
        this.folderId = folderId;
        this.serverId = serverId;
    }
}
