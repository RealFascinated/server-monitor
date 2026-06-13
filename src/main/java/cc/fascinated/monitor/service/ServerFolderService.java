package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.dto.request.server.ReorderServerFoldersRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return this.serverFolderRepository.findByUserIdOrderByPositionAsc(user.getId()).stream()
                .map(ServerFolderResponse::from)
                .toList();
    }

    @Transactional
    public ServerFolderResponse createFolder(UserRow user, ServerRenameRequest request) {
        if (this.serverFolderRepository.findByUserIdAndNameIgnoreCase(user.getId(), request.name()).isPresent()) {
            throw new ConflictException("A folder named \"%s\" already exists".formatted(request.name()));
        }
        int position = (int) this.serverFolderRepository.countByUserId(user.getId());
        return ServerFolderResponse.from(
                this.serverFolderRepository.save(new ServerFolderRow(request.name(), user.getId(), position))
        );
    }

    @Transactional
    public List<ServerFolderResponse> reorderFolders(UserRow user, ReorderServerFoldersRequest request) {
        List<ServerFolderRow> folders = this.serverFolderRepository.findByUserIdOrderByPositionAsc(user.getId());
        if (request.folderIds().size() != folders.size()) {
            throw new BadRequestException("Folder order must include every folder");
        }

        Map<Long, ServerFolderRow> foldersById = folders.stream()
                .collect(Collectors.toMap(ServerFolderRow::getId, Function.identity()));
        if (!foldersById.keySet().equals(new HashSet<>(request.folderIds()))) {
            throw new BadRequestException("Folder order contains unknown folders");
        }

        for (int index = 0; index < request.folderIds().size(); index++) {
            foldersById.get(request.folderIds().get(index)).setPosition(index);
        }

        this.serverFolderRepository.saveAll(folders);
        return listFolders(user);
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
                .orElseGet(() -> {
                    int position = (int) this.serverFolderRepository.countByUserId(user.getId());
                    return this.serverFolderRepository.save(
                            new ServerFolderRow(folderName, user.getId(), position)
                    );
                });

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
