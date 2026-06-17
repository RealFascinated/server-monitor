package cc.fascinated.monitor.server;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsWriteClient;
import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerMemberInviteRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
import cc.fascinated.monitor.model.dto.request.server.UpdateServerFolderRequest;
import cc.fascinated.monitor.model.dto.response.metrics.ServerMetricsResponse;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.dto.response.server.IncidentResponse;
import cc.fascinated.monitor.model.dto.response.server.IngestTokenResponse;
import cc.fascinated.monitor.model.dto.response.server.ServerCpuSnapshot;
import cc.fascinated.monitor.model.dto.response.server.ServerDiskSnapshot;
import cc.fascinated.monitor.model.dto.response.server.ServerFolderAssignmentResponse;
import cc.fascinated.monitor.model.dto.response.server.ServerMemorySnapshot;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.dto.response.server.ServerStatusResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerAccessListResponse;
import cc.fascinated.monitor.model.dto.response.server.access.ServerInviteCreatedResponse;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.server.access.ServerAccessService;
import cc.fascinated.monitor.server.metrics.ServerMetricService;
import cc.fascinated.monitor.service.InternetConnectivityService;
import cc.fascinated.monitor.util.NumberUtils;
import cc.fascinated.monitor.util.Pagination;
import cc.fascinated.monitor.util.ServerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ServerService {
    private final ServerRepository serverRepository;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final ServerMetricService serverMetricService;
    private final ServerAccessService serverAccessService;
    private final MonitorServerProperties serverProperties;
    private final InternetConnectivityService internetConnectivityService;
    private final ServerFolderService serverFolderService;
    private final IncidentService incidentService;
    private final ServerIngestService serverIngestService;

    public ServerService(ServerRepository serverRepository,
                         VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                         ServerMetricService serverMetricService,
                         ServerAccessService serverAccessService,
                         MonitorServerProperties serverProperties,
                         InternetConnectivityService internetConnectivityService,
                         ServerFolderService serverFolderService,
                         IncidentService incidentService,
                         ServerIngestService serverIngestService) {
        this.serverRepository = serverRepository;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.serverMetricService = serverMetricService;
        this.serverAccessService = serverAccessService;
        this.serverProperties = serverProperties;
        this.internetConnectivityService = internetConnectivityService;
        this.serverFolderService = serverFolderService;
        this.incidentService = incidentService;
        this.serverIngestService = serverIngestService;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    @Transactional
    public void markStaleServersOffline() {
        if (!this.internetConnectivityService.isAvailable()) {
            log.info("Skipping stale-server offline check — internet connectivity probe failed");
            return;
        }
        Instant cutoff = Instant.now().minus(this.serverProperties.getOfflineThreshold());
        List<ServerRow> staleServers = this.serverRepository.findStaleServers(cutoff, ServerStatus.ONLINE);
        if (staleServers.isEmpty()) {
            return;
        }
        int updated = this.serverRepository.markStaleServersOffline(cutoff, ServerStatus.OFFLINE, ServerStatus.ONLINE);
        if (updated > 0) {
            log.info("Marked {} server(s) offline (no update since {})", updated, cutoff);
            for (ServerRow server : staleServers) {
                this.incidentService.openOutage(server.getId(), server.getLastHeartbeat());
            }
            this.serverIngestService.writeServerStatusOffline(staleServers.stream().map(ServerRow::getId).toList());
        }
    }

    public List<ServerResponse> listServers(UserRow user) {
        List<ServerRow> servers = listAccessibleServers(user);
        if (servers.isEmpty()) {
            return List.of();
        }
        ServerResponseContext context = fetchServerResponseContext(servers, user.getId());
        Map<Long, ServerRole> rolesByServerId = this.serverAccessService.findRolesByUserId(user.getId());
        return servers.stream()
                .map(server -> toServerResponse(server, rolesByServerId.get(server.getId()), context))
                .toList();
    }

    public ServerResponse getServer(UserRow user, long serverId) {
        ServerRow server = requireServer(user, serverId, ServerPermission.VIEW_SERVER);
        ServerRole role = this.serverAccessService.findRole(server.getId(), user.getId()).orElseThrow();
        ServerResponseContext context = fetchServerResponseContext(List.of(server), user.getId());
        return toServerResponse(server, role, context);
    }

    public ServerStatusResponse getServerStatus(UserRow user, long serverId) {
        ServerRow server = requireServer(user, serverId, ServerPermission.VIEW_SERVER);
        return ServerStatusResponse.from(server);
    }

    public Pagination.Page<IncidentResponse> listIncidents(UserRow user, long serverId, int page, int count) {
        requireServer(user, serverId, ServerPermission.VIEW_SERVER);
        return this.incidentService.listIncidents(serverId, page, count);
    }

    @Transactional
    public ServerFolderAssignmentResponse updateServerFolder(
            UserRow user,
            long serverId,
            UpdateServerFolderRequest request
    ) {
        ServerRow server = requireServer(user, serverId, ServerPermission.ASSIGN_FOLDER);
        String folderName = this.serverFolderService.updateServerFolder(user, server, request);
        return new ServerFolderAssignmentResponse(serverId, folderName);
    }

    public ServerMetricsResponse getServerMetrics(UserRow user, long serverId, MetricQueryWindow window) {
        ServerRow server = requireServer(user, serverId, ServerPermission.VIEW_METRICS);
        return this.serverMetricService.getServerMetrics(server, window);
    }

    public ServerAccessListResponse listMembers(UserRow user, long serverId) {
        return this.serverAccessService.listAccess(user, findServerRowById(serverId));
    }

    public ServerInviteCreatedResponse inviteMember(UserRow user, long serverId, ServerMemberInviteRequest request) {
        return this.serverAccessService.inviteUser(user, findServerRowById(serverId), request);
    }

    public void removeMember(UserRow user, long serverId, long memberUserId) {
        this.serverAccessService.removeMember(user, findServerRowById(serverId), memberUserId);
    }

    public void leaveServer(UserRow user, long serverId) {
        this.serverAccessService.leaveServer(user, findServerRowById(serverId));
    }

    public void revokeInvite(UserRow user, long serverId, long inviteId) {
        this.serverAccessService.revokeInvite(user, findServerRowById(serverId), inviteId);
    }

    @Transactional
    public CreatedServerResponse createServer(UserRow user, ServerCreateRequest createRequest) {
        try {
            Instant now = Instant.now();
            ServerRow serverRow = this.serverRepository.save(new ServerRow(createRequest.name(), now));
            this.serverAccessService.addOwner(serverRow.getId(), user.getId(), now);
            UUID ingestToken = this.serverIngestService.issueIngestToken(serverRow.getId());

            String folderName = createRequest.folderName();
            if (folderName != null && !folderName.isBlank()) {
                this.serverFolderService.updateServerFolder(
                        user,
                        serverRow,
                        new UpdateServerFolderRequest(folderName.trim())
                );
            }

            return new CreatedServerResponse(
                    serverRow.getServerName(),
                    serverRow.getId(),
                    ingestToken
            );
        } catch (Exception ex) {
            log.error("Failed to create the server \"{}\"", createRequest.name(), ex);
            throw new InternalServerException("Failed to create the server \"%s\"".formatted(createRequest.name()));
        }
    }

    public ServerRow findServerRowById(long id) {
        Optional<ServerRow> serverRowOptional = this.serverRepository.findById(id);
        if (serverRowOptional.isPresent()) {
            return serverRowOptional.get();
        }
        throw new NotFoundException("Server \"%s\" not found".formatted(id));
    }

    public ServerRow requireServer(UserRow user, long serverId, ServerPermission permission) {
        ServerRow server = findServerRowById(serverId);
        this.serverAccessService.requirePermission(user, server, permission);
        return server;
    }

    @Transactional
    public ServerResponse renameServer(UserRow user, long serverId, ServerRenameRequest request) {
        ServerRow server = requireServer(user, serverId, ServerPermission.RENAME_SERVER);
        server.setServerName(request.name());
        this.serverRepository.save(server);
        ServerRole role = this.serverAccessService.findRole(server.getId(), user.getId()).orElseThrow();
        ServerResponseContext context = fetchServerResponseContext(List.of(server), user.getId());
        return toServerResponse(server, role, context);
    }

    @Transactional
    public void deleteServer(UserRow user, long serverId) {
        requireServer(user, serverId, ServerPermission.DELETE_SERVER);
        this.victoriaMetricsWriteClient.deleteSeriesForServer(serverId);
        this.serverRepository.deleteById(serverId);
        log.info("Deleted server {} and its VictoriaMetrics series", serverId);
    }

    @Transactional
    public IngestTokenResponse rotateIngestToken(UserRow user, long serverId) {
        requireServer(user, serverId, ServerPermission.ROTATE_INGEST_TOKEN);
        UUID ingestToken = this.serverIngestService.rotateIngestToken(serverId);
        return new IngestTokenResponse(serverId, ingestToken);
    }

    private List<ServerRow> listAccessibleServers(UserRow user) {
        List<Long> serverIds = this.serverAccessService.findAccessibleServerIds(user.getId());
        if (serverIds.isEmpty()) {
            return List.of();
        }
        List<ServerRow> servers = new ArrayList<>(this.serverRepository.findAllById(serverIds));
        servers.sort(ServerUtils.NAME_ORDER);
        return servers;
    }

    private static final String ROOT_DISK_MOUNT = "/";
    private static final Map<String, String> ROOT_DISK_LABEL = Map.of("disk", ROOT_DISK_MOUNT);

    private ServerResponseContext fetchServerResponseContext(List<ServerRow> servers, long userId) {
        List<Long> serverIds = servers.stream().map(ServerRow::getId).toList();
        List<Long> onlineServerIds = servers.stream()
                .filter(server -> server.getStatus() == ServerStatus.ONLINE)
                .map(ServerRow::getId)
                .toList();
        return new ServerResponseContext(
                fetchLatestHostMetrics(onlineServerIds),
                this.serverMetricService.fetchUptimePercent30d(serverIds),
                this.serverFolderService.findFolderNamesByServerIds(userId, serverIds)
        );
    }

    private ServerResponse toServerResponse(ServerRow server, ServerRole role, ServerResponseContext context) {
        Double uptime = context.uptimePercent30d().get(server.getId());
        String folderName = context.folderNamesByServerId().get(server.getId());
        if (server.getStatus() != ServerStatus.ONLINE) {
            return ServerResponse.from(server, role, null, null, null, uptime, folderName);
        }
        long serverId = server.getId();
        LatestHostMetrics metrics = context.hostMetrics();
        return ServerResponse.from(
                server,
                role,
                new ServerCpuSnapshot(
                        metrics.cpuUsage().get(serverId),
                        metrics.cpuUser().get(serverId),
                        metrics.cpuSystem().get(serverId),
                        metrics.cpuIowait().get(serverId),
                        metrics.cpuSteal().get(serverId)
                ),
                new ServerMemorySnapshot(
                        NumberUtils.toLong(metrics.memUsage().get(serverId)),
                        NumberUtils.toLong(metrics.memMax().get(serverId))
                ),
                new ServerDiskSnapshot(
                        NumberUtils.toLong(metrics.diskUsage().get(serverId)),
                        NumberUtils.toLong(metrics.diskMax().get(serverId))
                ),
                uptime,
                folderName
        );
    }

    private LatestHostMetrics fetchLatestHostMetrics(List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return LatestHostMetrics.empty();
        }
        Map<VmGaugeSeries, Map<Long, Double>> latest =
                this.serverMetricService.fetchLatestMetrics(
                        List.of(
                                HostSeries.CPU_USAGE,
                                HostSeries.CPU_USER_PCT,
                                HostSeries.CPU_SYSTEM_PCT,
                                HostSeries.CPU_IOWAIT_PCT,
                                HostSeries.CPU_STEAL_PCT,
                                HostSeries.MEM_USAGE,
                                HostSeries.MEM_TOTAL,
                                DiskSeries.USED_BYTES,
                                DiskSeries.TOTAL_BYTES
                        ),
                        serverIds,
                        Map.of(
                                DiskSeries.USED_BYTES, ROOT_DISK_LABEL,
                                DiskSeries.TOTAL_BYTES, ROOT_DISK_LABEL
                        )
                );
        return new LatestHostMetrics(
                latest.getOrDefault(HostSeries.CPU_USAGE, Map.of()),
                latest.getOrDefault(HostSeries.CPU_USER_PCT, Map.of()),
                latest.getOrDefault(HostSeries.CPU_SYSTEM_PCT, Map.of()),
                latest.getOrDefault(HostSeries.CPU_IOWAIT_PCT, Map.of()),
                latest.getOrDefault(HostSeries.CPU_STEAL_PCT, Map.of()),
                latest.getOrDefault(HostSeries.MEM_USAGE, Map.of()),
                latest.getOrDefault(HostSeries.MEM_TOTAL, Map.of()),
                latest.getOrDefault(DiskSeries.USED_BYTES, Map.of()),
                latest.getOrDefault(DiskSeries.TOTAL_BYTES, Map.of())
        );
    }

    private record ServerResponseContext(
            LatestHostMetrics hostMetrics,
            Map<Long, Double> uptimePercent30d,
            Map<Long, String> folderNamesByServerId
    ) {}

    private record LatestHostMetrics(
            Map<Long, Double> cpuUsage,
            Map<Long, Double> cpuUser,
            Map<Long, Double> cpuSystem,
            Map<Long, Double> cpuIowait,
            Map<Long, Double> cpuSteal,
            Map<Long, Double> memUsage,
            Map<Long, Double> memMax,
            Map<Long, Double> diskUsage,
            Map<Long, Double> diskMax
    ) {
        private static LatestHostMetrics empty() {
            return new LatestHostMetrics(
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        }
    }
}
