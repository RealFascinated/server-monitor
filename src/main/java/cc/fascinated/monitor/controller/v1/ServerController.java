package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerMemberInviteRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
import cc.fascinated.monitor.model.dto.request.server.UpdateServerFolderRequest;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.dto.response.server.IngestTokenResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerAccessListResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerInviteCreatedResponse;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.ServerAccessService;
import cc.fascinated.monitor.service.ServerService;
import cc.fascinated.monitor.web.auth.AuthenticatedServer;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/servers")
public class ServerController {

    private final ServerService serverService;
    private final ServerAccessService serverAccessService;

    public ServerController(ServerService serverService, ServerAccessService serverAccessService) {
        this.serverService = serverService;
        this.serverAccessService = serverAccessService;
    }

    @GetMapping(value = "/{serverId}")
    public ServerResponse getServer(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        return this.serverService.getServer(user, serverId);
    }

    @PostMapping(value = "/create")
    public CreatedServerResponse createServer(@AuthenticatedUser UserRow user, @RequestBody ServerCreateRequest createRequest) {
        return this.serverService.createServer(user, createRequest);
    }

    @PostMapping(value = "/{serverId}/rename")
    public ServerResponse renameServer(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @Valid @RequestBody ServerRenameRequest request
    ) {
        return this.serverService.renameServer(user, serverId, request);
    }

    @PostMapping(value = "/{serverId}/folder")
    public ServerResponse updateServerFolder(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @Valid @RequestBody UpdateServerFolderRequest request
    ) {
        return this.serverService.updateServerFolder(user, serverId, request);
    }

    @DeleteMapping(value = "/{serverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServer(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        this.serverService.deleteServer(user, serverId);
    }

    @PostMapping(value = "/{serverId}/ingest-token/rotate")
    public IngestTokenResponse rotateIngestToken(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        return this.serverService.rotateIngestToken(user, serverId);
    }

    @GetMapping(value = "/{serverId}/members")
    public ServerAccessListResponse listMembers(@AuthenticatedUser UserRow user, @PathVariable long serverId) {
        return this.serverAccessService.listAccess(user, this.serverService.getAccessibleServer(user, serverId));
    }

    @PostMapping(value = "/{serverId}/members/invite")
    public ServerInviteCreatedResponse inviteMember(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @Valid @RequestBody ServerMemberInviteRequest request
    ) {
        return this.serverAccessService.inviteUser(user, this.serverService.requireOwnedServer(user, serverId), request);
    }

    @DeleteMapping(value = "/{serverId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @PathVariable long memberUserId
    ) {
        this.serverAccessService.removeMember(user, this.serverService.requireOwnedServer(user, serverId), memberUserId);
    }

    @DeleteMapping(value = "/{serverId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @PathVariable long inviteId
    ) {
        this.serverAccessService.revokeInvite(user, this.serverService.requireOwnedServer(user, serverId), inviteId);
    }

    @PostMapping(value = "/heartbeat")
    public void heartbeat(@AuthenticatedServer ServerRow server) {
        this.serverService.recordHeartbeat(server);
    }

    @PostMapping(value = "/ingest")
    public void ingestMetrics(@AuthenticatedServer ServerRow server, @RequestBody IngestServerMetrics metrics,
                              HttpServletRequest request) {
        long payloadBytes = request.getContentLengthLong();
        if (payloadBytes < 0) {
            payloadBytes = 0;
        }
        this.serverService.ingestMetrics(server, metrics, payloadBytes);
    }
}
