package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.dto.request.server.ServerInviteAcceptRequest;
import cc.fascinated.monitor.model.dto.request.user.ChangePasswordRequest;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerInvitePreviewResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerMemberResponse;
import cc.fascinated.monitor.model.dto.response.server.access.UserPendingInviteResponse;
import cc.fascinated.monitor.model.dto.response.metrics.ServerMetricsResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import cc.fascinated.monitor.server.access.ServerAccessService;
import cc.fascinated.monitor.service.PasswordService;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.web.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/user")
public class UserController {
    private final ServerService serverService;
    private final ServerAccessService serverAccessService;
    private final PasswordService passwordService;

    public UserController(
            ServerService serverService,
            ServerAccessService serverAccessService,
            PasswordService passwordService
    ) {
        this.serverService = serverService;
        this.serverAccessService = serverAccessService;
        this.passwordService = passwordService;
    }

    @PatchMapping(value = "/password")
    public void changePassword(
            @AuthenticatedUser UserRow user,
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest body
    ) {
        this.passwordService.changePassword(
                user,
                body.currentPassword(),
                body.newPassword(),
                AuthUtils.extractBearerValue(request.getHeader("Authorization"))
        );
    }

    @GetMapping(value = "/servers")
    public List<ServerResponse> listServers(@AuthenticatedUser UserRow user) {
        return this.serverService.listServers(user);
    }

    @GetMapping(value = "/servers/{serverId}/metrics")
    public ServerMetricsResponse getServerMetrics(
            @AuthenticatedUser UserRow user,
            @PathVariable long serverId,
            @RequestParam long from,
            @RequestParam long to
    ) {
        return this.serverService.getServerMetrics(
                user,
                serverId,
                MetricQueryWindow.parse(from, to)
        );
    }

    @GetMapping(value = "/invites")
    public List<UserPendingInviteResponse> listInvites(@AuthenticatedUser UserRow user) {
        return this.serverAccessService.listPendingInvites(user);
    }

    @GetMapping(value = "/invites/preview")
    public ServerInvitePreviewResponse previewInvite(
            @AuthenticatedUser UserRow user,
            @RequestParam String token
    ) {
        return this.serverAccessService.previewInvite(token);
    }

    @PostMapping(value = "/invites/accept")
    public ServerMemberResponse acceptInvite(
            @AuthenticatedUser UserRow user,
            @Valid @RequestBody ServerInviteAcceptRequest request
    ) {
        return this.serverAccessService.acceptInvite(user, request.token());
    }

    @PostMapping(value = "/invites/{inviteId}/accept")
    public ServerMemberResponse acceptInviteById(
            @AuthenticatedUser UserRow user,
            @PathVariable long inviteId
    ) {
        return this.serverAccessService.acceptInviteById(user, inviteId);
    }
}
