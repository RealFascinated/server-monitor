package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
import cc.fascinated.monitor.model.dto.request.server.UpdateServerFolderRequest;
import cc.fascinated.monitor.model.dto.response.server.ServerFolderResponse;
import cc.fascinated.monitor.model.persistance.ServerFolderAssignmentRow;
import cc.fascinated.monitor.model.persistance.ServerFolderRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.ServerFolderAssignmentRepository;
import cc.fascinated.monitor.repository.ServerFolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServerFolderService {
    private final ServerFolderRepository serverFolderRepository;
    private final ServerFolderAssignmentRepository serverFolderAssignmentRepository;

    public ServerFolderService(ServerFolderRepository serverFolderRepository,
                               ServerFolderAssignmentRepository serverFolderAssignmentRepository) {
        this.serverFolderRepository = serverFolderRepository;
        this.serverFolderAssignmentRepository = serverFolderAssignmentRepository;
    }

    public List<ServerFolderResponse> listFolders(UserRow user) {
        return this.serverFolderRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .map(ServerFolderResponse::from)
                .toList();
    }

    public Map<Long, String> findFolderNamesByServerIds(long userId, List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> folderNamesByServerId = new HashMap<>();
        for (Object[] row : this.serverFolderAssignmentRepository.findFolderNamesByUserIdAndServerIds(userId, serverIds)) {
            folderNamesByServerId.put((Long) row[0], (String) row[1]);
        }
        return folderNamesByServerId;
    }

    @Transactional
    public void updateServerFolder(UserRow user, ServerRow server, UpdateServerFolderRequest request) {
        String folderName = request.folderName();
        if (folderName == null || folderName.isBlank()) {
            this.serverFolderAssignmentRepository.deleteByServerIdAndUserId(server.getId(), user.getId());
            return;
        }

        ServerFolderRow folder = this.serverFolderRepository
                .findByUserIdAndNameIgnoreCase(user.getId(), folderName)
                .orElseGet(() -> this.serverFolderRepository.save(new ServerFolderRow(folderName, user.getId())));

        this.serverFolderAssignmentRepository.deleteByServerIdAndUserId(server.getId(), user.getId());
        this.serverFolderAssignmentRepository.save(new ServerFolderAssignmentRow(folder.getId(), server.getId()));
    }

    @Transactional
    public ServerFolderResponse renameFolder(UserRow user, long folderId, ServerRenameRequest request) {
        ServerFolderRow folder = requireUserFolder(user, folderId);
        if (this.serverFolderRepository.findByUserIdAndNameIgnoreCaseExcludingId(
                user.getId(), request.name(), folderId
        ).isPresent()) {
            throw new ConflictException("A folder named \"%s\" already exists".formatted(request.name()));
        }
        folder.setName(request.name());
        return ServerFolderResponse.from(this.serverFolderRepository.save(folder));
    }

    @Transactional
    public void deleteFolder(UserRow user, long folderId) {
        ServerFolderRow folder = requireUserFolder(user, folderId);
        this.serverFolderRepository.delete(folder);
    }

    private ServerFolderRow requireUserFolder(UserRow user, long folderId) {
        return this.serverFolderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new NotFoundException("Folder \"%s\" not found".formatted(folderId)));
    }
}
