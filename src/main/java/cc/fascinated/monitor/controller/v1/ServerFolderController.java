package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
import cc.fascinated.monitor.model.dto.response.server.ServerFolderResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.ServerFolderService;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/user/server-folders")
public class ServerFolderController {
    private final ServerFolderService serverFolderService;

    public ServerFolderController(ServerFolderService serverFolderService) {
        this.serverFolderService = serverFolderService;
    }

    @GetMapping
    public List<ServerFolderResponse> listFolders(@AuthenticatedUser UserRow user) {
        return this.serverFolderService.listFolders(user);
    }

    @PostMapping
    public ServerFolderResponse createFolder(
            @AuthenticatedUser UserRow user,
            @Valid @RequestBody ServerRenameRequest request
    ) {
        return this.serverFolderService.createFolder(user, request);
    }

    @PostMapping(value = "/{folderId}/rename")
    public ServerFolderResponse renameFolder(
            @AuthenticatedUser UserRow user,
            @PathVariable long folderId,
            @Valid @RequestBody ServerRenameRequest request
    ) {
        return this.serverFolderService.renameFolder(user, folderId, request);
    }

    @DeleteMapping(value = "/{folderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFolder(@AuthenticatedUser UserRow user, @PathVariable long folderId) {
        this.serverFolderService.deleteFolder(user, folderId);
    }
}
