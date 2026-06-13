package cc.fascinated.monitor.service;

import cc.fascinated.monitor.model.domain.websocket.WebSocketCommand;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.dto.response.server.access.PendingInviteResponse;
import cc.fascinated.monitor.model.dto.response.server.access.UserPendingInviteResponse;
import cc.fascinated.monitor.model.dto.websocket.*;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.ServerMemberRepository;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.websocket.WebSocketAuthHandshakeInterceptor;
import cc.fascinated.monitor.websocket.impl.ServersWebSocket;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class ServerWebSocketService {

    private static final long FLUSH_INTERVAL_MS = 1_500;

    private final ServersWebSocket serversWebSocket;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final ServerAccessService serverAccessService;
    private final ServerService serverService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> pendingServerUpdatesByUserId = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> pendingMetricsUpdatesByUserId = new ConcurrentHashMap<>();
    private final Set<Long> scheduledFlushUserIds = ConcurrentHashMap.newKeySet();

    public ServerWebSocketService(
            @Lazy ServersWebSocket serversWebSocket,
            ServerMemberRepository serverMemberRepository,
            UserRepository userRepository,
            ServerAccessService serverAccessService,
            @Lazy ServerService serverService
    ) {
        this.serversWebSocket = serversWebSocket;
        this.serverMemberRepository = serverMemberRepository;
        this.userRepository = userRepository;
        this.serverAccessService = serverAccessService;
        this.serverService = serverService;
    }

    @PreDestroy
    public void shutdown() {
        this.scheduler.shutdownNow();
        this.executor.shutdownNow();
    }

    public void registerSession(long userId, WebSocketSession session) {
        this.sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterSession(long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = this.sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            this.sessionsByUserId.remove(userId);
            this.pendingServerUpdatesByUserId.remove(userId);
            this.pendingMetricsUpdatesByUserId.remove(userId);
            this.scheduledFlushUserIds.remove(userId);
        }
    }

    public int getConnectionCount() {
        int count = 0;
        for (Set<WebSocketSession> sessions : this.sessionsByUserId.values()) {
            count += sessions.size();
        }
        return count;
    }

    public void notifyIngest(long serverId) {
        runAsync(() -> {
            for (long userId : memberUserIds(serverId)) {
                queueServerUpdate(userId, serverId);
                queueMetricsUpdate(userId, serverId);
            }
        });
    }

    public void notifyServerCreated(UserRow user, long serverId) {
        runAsync(() -> sendServerCreated(user.getId(), serverId));
    }

    public void notifyServerUpdated(long serverId) {
        runAsync(() -> {
            for (long userId : memberUserIds(serverId)) {
                queueServerUpdate(userId, serverId);
            }
        });
    }

    public void notifyServerDeleted(long serverId, List<Long> memberUserIds) {
        runAsync(() -> {
            ServerIdData data = new ServerIdData(serverId);
            for (long userId : memberUserIds) {
                sendToUser(userId, WebSocketCommand.SERVER_DELETED, data);
            }
        });
    }

    public void notifyMemberRemoved(long serverId, long removedUserId) {
        runAsync(() -> {
            sendToUser(removedUserId, WebSocketCommand.SERVER_DELETED, new ServerIdData(serverId));
            notifyMemberChange(serverId);
        });
    }

    public void notifyMemberChange(long serverId) {
        runAsync(() -> sendMemberChangeToMembers(serverId));
    }

    public void notifyInviteAccepted(long serverId, long newMemberId, long inviteId) {
        runAsync(() -> {
            queueServerUpdate(newMemberId, serverId);
            flushImmediately(newMemberId);
            sendToUser(newMemberId, WebSocketCommand.INVITE_REVOKED, new InviteRevokedInviteeData(inviteId));
            sendMemberChangeToMembers(serverId);
        });
    }

    public void notifyInviteCreated(ServerRow server, ServerInviteRow invite) {
        runAsync(() -> {
            UserRow owner = this.serverAccessService.getOwnerUser(server);
            sendToUser(
                    owner.getId(),
                    WebSocketCommand.INVITE_CREATED,
                    new InviteCreatedOwnerData(server.getId(), PendingInviteResponse.from(invite))
            );
            this.userRepository.findByEmailIgnoreCase(invite.getEmail()).ifPresent(invitee -> sendToUser(
                    invitee.getId(),
                    WebSocketCommand.INVITE_CREATED,
                    new InviteCreatedInviteeData(UserPendingInviteResponse.from(invite, server))
            ));
        });
    }

    public void notifyInviteRevoked(ServerRow server, ServerInviteRow invite) {
        runAsync(() -> {
            UserRow owner = this.serverAccessService.getOwnerUser(server);
            sendToUser(
                    owner.getId(),
                    WebSocketCommand.INVITE_REVOKED,
                    new InviteRevokedOwnerData(server.getId(), invite.getId())
            );
            this.userRepository.findByEmailIgnoreCase(invite.getEmail()).ifPresent(invitee -> sendToUser(
                    invitee.getId(),
                    WebSocketCommand.INVITE_REVOKED,
                    new InviteRevokedInviteeData(invite.getId())
            ));
        });
    }

    public void notifyServersOffline(List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return;
        }
        runAsync(() -> {
            for (long serverId : serverIds) {
                for (long userId : memberUserIds(serverId)) {
                    queueServerUpdate(userId, serverId);
                }
            }
        });
    }

    private void queueServerUpdate(long userId, long serverId) {
        this.pendingServerUpdatesByUserId
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(serverId);
        scheduleFlush(userId);
    }

    private void queueMetricsUpdate(long userId, long serverId) {
        this.pendingMetricsUpdatesByUserId
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(serverId);
        scheduleFlush(userId);
    }

    private void scheduleFlush(long userId) {
        if (this.scheduledFlushUserIds.add(userId)) {
            this.scheduler.schedule(() -> flushUser(userId), FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flushImmediately(long userId) {
        this.scheduledFlushUserIds.remove(userId);
        flushUser(userId);
    }

    private void flushUser(long userId) {
        this.scheduledFlushUserIds.remove(userId);

        Set<Long> serverIds = this.pendingServerUpdatesByUserId.remove(userId);
        Set<Long> metricsServerIds = this.pendingMetricsUpdatesByUserId.remove(userId);

        if (serverIds != null && !serverIds.isEmpty()) {
            UserRow user = this.userRepository.findById(userId).orElse(null);
            if (user != null) {
                List<ServerResponse> servers = new ArrayList<>(serverIds.size());
                for (long serverId : serverIds) {
                    try {
                        servers.add(this.serverService.getServer(user, serverId));
                    } catch (RuntimeException ex) {
                        log.debug(
                                "Skipping SERVERS_UPDATE for user {} server {}: {}",
                                userId,
                                serverId,
                                ex.getMessage()
                        );
                    }
                }
                if (!servers.isEmpty()) {
                    sendToUser(userId, WebSocketCommand.SERVERS_UPDATE, new ServersUpdateData(servers));
                }
            }
        }

        if (metricsServerIds != null && !metricsServerIds.isEmpty()) {
            sendToUser(
                    userId,
                    WebSocketCommand.SERVER_METRICS_UPDATED,
                    new ServerMetricsUpdatedData(new ArrayList<>(metricsServerIds))
            );
        }

        if (hasPendingUpdates(userId)) {
            scheduleFlush(userId);
        }
    }

    private boolean hasPendingUpdates(long userId) {
        Set<Long> pendingServers = this.pendingServerUpdatesByUserId.get(userId);
        if (pendingServers != null && !pendingServers.isEmpty()) {
            return true;
        }
        Set<Long> pendingMetrics = this.pendingMetricsUpdatesByUserId.get(userId);
        return pendingMetrics != null && !pendingMetrics.isEmpty();
    }

    private void sendServerCreated(long userId, long serverId) {
        UserRow user = this.userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        ServerResponse response = this.serverService.getServer(user, serverId);
        sendToUser(userId, WebSocketCommand.SERVER_CREATED, response);
    }

    private void sendMemberChangeToMembers(long serverId) {
        ServerRow server = this.serverService.findServerRowById(serverId);
        for (ServerMemberRow member : this.serverMemberRepository.findByServerId(serverId)) {
            UserRow user = this.userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            MemberChangeData data = new MemberChangeData(serverId, this.serverAccessService.listAccess(user, server));
            sendToUser(user.getId(), WebSocketCommand.MEMBER_CHANGE, data);
        }
    }

    private List<Long> memberUserIds(long serverId) {
        return this.serverMemberRepository.findByServerId(serverId).stream()
                .map(ServerMemberRow::getUserId)
                .toList();
    }

    private void sendToUser(long userId, WebSocketCommand command, Object data) {
        Set<WebSocketSession> sessions = this.sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        WebSocketMessage message = new WebSocketMessage(command.name(), data);
        for (WebSocketSession session : sessions) {
            this.serversWebSocket.sendMessage(session, message);
        }
    }

    private void runAsync(Runnable task) {
        this.executor.execute(task);
    }

    public static long userIdFromSession(WebSocketSession session) {
        return (long) session.getAttributes().get(WebSocketAuthHandshakeInterceptor.USER_ID_ATTRIBUTE);
    }
}
