package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
import cc.fascinated.monitor.model.dto.request.server.UpdateServerFolderRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.dto.response.server.IngestTokenResponse;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.persistance.ServerInventoryRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.model.persistance.metric.IngestTokenRow;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsWriteClient;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.server.series.CpuCoreSeries;
import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.DockerSeries;
import cc.fascinated.monitor.metrics.server.series.GpuSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.metrics.server.series.TemperatureSeries;
import cc.fascinated.monitor.metrics.server.series.NetworkSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsArcSeries;
import cc.fascinated.monitor.metrics.server.series.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsPoolSeries;
import cc.fascinated.monitor.repository.ServerIngestTokenRepository;
import cc.fascinated.monitor.repository.ServerMemberRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.NumberUtils;
import cc.fascinated.monitor.util.ServerUtils;
import cc.fascinated.monitor.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private final ServerMemberRepository serverMemberRepository;
    private final ServerIngestTokenRepository serverIngestTokenRepository;
    private final PlatformMetricsRecorder platformMetricsRecorder;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final ServerMetricService serverMetricService;
    private final ServerAccessService serverAccessService;
    private final MonitorServerProperties serverProperties;
    private final ServerWebSocketService serverWebSocketService;
    private final InternetConnectivityService internetConnectivityService;
    private final ServerFolderService serverFolderService;

    public ServerService(ServerRepository serverRepository, ServerMemberRepository serverMemberRepository,
                         ServerIngestTokenRepository serverIngestTokenRepository,
                         PlatformMetricsRecorder platformMetricsRecorder,
                         VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                         ServerMetricService serverMetricService,
                         ServerAccessService serverAccessService,
                         MonitorServerProperties serverProperties,
                         @Lazy ServerWebSocketService serverWebSocketService,
                         InternetConnectivityService internetConnectivityService,
                         ServerFolderService serverFolderService) {
        this.serverRepository = serverRepository;
        this.serverMemberRepository = serverMemberRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.platformMetricsRecorder = platformMetricsRecorder;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.serverMetricService = serverMetricService;
        this.serverAccessService = serverAccessService;
        this.serverProperties = serverProperties;
        this.serverWebSocketService = serverWebSocketService;
        this.internetConnectivityService = internetConnectivityService;
        this.serverFolderService = serverFolderService;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    @Transactional
    public void markStaleServersOffline() {
        if (!this.internetConnectivityService.isAvailable()) {
            log.info("Skipping stale-server offline check — internet connectivity probe failed");
            return;
        }
        Instant cutoff = Instant.now().minus(this.serverProperties.getOfflineThreshold());
        List<Long> staleServerIds = this.serverRepository.findStaleServerIds(cutoff);
        if (staleServerIds.isEmpty()) {
            return;
        }
        int updated = this.serverRepository.markStaleServersOffline(cutoff);
        if (updated > 0) {
            log.info("Marked {} server(s) offline (no update since {})", updated, cutoff);
            writeServerStatusOffline(staleServerIds);
            this.serverWebSocketService.notifyServersOffline(staleServerIds);
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
        ServerRow server = getAccessibleServer(user, serverId);
        ServerRole role = this.serverAccessService.findRole(server.getId(), user.getId()).orElseThrow();
        ServerResponseContext context = fetchServerResponseContext(List.of(server), user.getId());
        return toServerResponse(server, role, context);
    }

    @Transactional
    public ServerResponse updateServerFolder(UserRow user, long serverId, UpdateServerFolderRequest request) {
        ServerRow server = getAccessibleServer(user, serverId);
        this.serverFolderService.updateServerFolder(user, server, request);
        return getServer(user, serverId);
    }

    @Transactional
    public CreatedServerResponse createServer(UserRow user, ServerCreateRequest createRequest) {
        try {
            Instant now = Instant.now();
            ServerRow serverRow = this.serverRepository.save(new ServerRow(createRequest.name(), now));
            this.serverAccessService.addOwner(serverRow.getId(), user.getId(), now);
            UUID ingestToken = issueIngestToken(serverRow.getId());

            CreatedServerResponse response = new CreatedServerResponse(
                    serverRow.getServerName(),
                    serverRow.getId(),
                    ingestToken
            );
            this.serverWebSocketService.notifyServerCreated(user, serverRow.getId());
            return response;
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

    public ServerRow getAccessibleServer(UserRow user, long serverId) {
        ServerRow server = findServerRowById(serverId);
        this.serverAccessService.requireAccessible(user, server);
        return server;
    }

    public ServerRow requireOwnedServer(UserRow user, long serverId) {
        ServerRow server = getAccessibleServer(user, serverId);
        this.serverAccessService.requireOwner(user, server);
        return server;
    }

    @Transactional
    public ServerResponse renameServer(UserRow user, long serverId, ServerRenameRequest request) {
        ServerRow server = requireOwnedServer(user, serverId);
        server.setServerName(request.name());
        this.serverRepository.save(server);
        this.serverWebSocketService.notifyServerUpdated(serverId);
        return getServer(user, serverId);
    }

    @Transactional
    public void deleteServer(UserRow user, long serverId) {
        requireOwnedServer(user, serverId);
        List<Long> memberUserIds = this.serverMemberRepository.findByServerId(serverId).stream()
                .map(ServerMemberRow::getUserId)
                .toList();
        this.serverWebSocketService.notifyServerDeleted(serverId, memberUserIds);
        this.victoriaMetricsWriteClient.deleteSeriesForServer(serverId);
        this.serverRepository.deleteById(serverId);
        log.info("Deleted server {} and its VictoriaMetrics series", serverId);
    }

    @Transactional
    public IngestTokenResponse rotateIngestToken(UserRow user, long serverId) {
        requireOwnedServer(user, serverId);
        this.serverIngestTokenRepository.deleteByServerId(serverId);
        UUID ingestToken = issueIngestToken(serverId);
        log.info("Rotated ingest token for server {}", serverId);
        return new IngestTokenResponse(serverId, ingestToken);
    }

    public ServerRow authenticateIngestRequest(String authorizationHeader) {
        try {
            UUID token = AuthUtils.parseBearerToken(authorizationHeader);
            IngestTokenRow tokenRow = this.serverIngestTokenRepository.findByTokenHash(AuthUtils.hash(token.toString()))
                    .orElseThrow(() -> new UnauthorizedException("Invalid ingest token"));
            return findServerRowById(tokenRow.getServerId());
        } catch (UnauthorizedException ex) {
            this.platformMetricsRecorder.recordIngestAuthFailure();
            throw ex;
        }
    }

    @Transactional
    public void recordHeartbeat(ServerRow server) {
        Instant now = Instant.now();
        server.setLastHeartbeat(now);
        if (server.getStatus() != ServerStatus.ONLINE) {
            server.setStatus(ServerStatus.ONLINE);
        }
        this.serverRepository.save(server);
        this.serverWebSocketService.notifyServerUpdated(server.getId());
    }

    @Transactional
    public void ingestMetrics(ServerRow server, IngestServerMetrics metrics, long payloadBytes) {
        long startedNanos = System.nanoTime();

        Instant now = Instant.now();

        // todo: verify agent version
        server.setAgentVersion(metrics.agentVersion());

        ServerDetails serverDetails = metrics.serverDetails();
        ServerInventoryRow inventory = getOrCreateInventory(server);
        inventory.setIp(serverDetails.ip());
        inventory.setCoreCount(serverDetails.coreCount());
        inventory.setThreadCount(serverDetails.threadCount());
        inventory.setOsName(serverDetails.osName());
        inventory.setOsVersion(serverDetails.osVersion());
        inventory.setCpuModel(serverDetails.cpuModel());
        inventory.setSocketCount(serverDetails.socketCount());
        server.setLastUptimeSeconds(serverDetails.uptimeSeconds());
        server.setLastUpdated(now);
        server.setLastHeartbeat(now);
        server.setStatus(ServerStatus.ONLINE);

        StringBuilder buffer = new StringBuilder();
        MetricWriteContext ctx = new MetricWriteContext(buffer, server.getId(), now.getEpochSecond());
        ServerMetrics serverMetrics = metrics.serverMetrics();
        HostSeries.write(ctx, serverMetrics, metrics.serverDetails());
        Utils.forEach(serverMetrics.cpuCoreMetrics(), core -> CpuCoreSeries.write(ctx, core));
        Utils.forEach(serverMetrics.temperatureMetrics(), reading -> TemperatureSeries.write(ctx, reading));
        Utils.forEach(metrics.interfaceMetrics(), iface -> NetworkSeries.write(ctx, iface));
        Utils.forEach(metrics.diskMetrics(), disk -> DiskSeries.write(ctx, disk));
        Utils.forEach(metrics.gpuMetrics(), gpu -> GpuSeries.write(ctx, gpu));
        if (metrics.zfsArcMetrics() != null) {
            ZfsArcSeries.write(ctx, metrics.zfsArcMetrics());
        }
        Utils.forEach(metrics.zfsPoolMetrics(), pool -> ZfsPoolSeries.write(ctx, pool));
        Utils.forEach(metrics.dockerContainers(), container -> DockerSeries.write(ctx, container));
        Utils.forEach(metrics.tcpConnectionMetrics(), tcp -> TcpConnectionSeries.write(ctx, tcp));
        ServerStatusSeries.writeOnline(ctx);
        this.victoriaMetricsWriteClient.flush(buffer.toString());

        this.serverRepository.save(server);
        this.serverWebSocketService.notifyIngest(server.getId());

        this.platformMetricsRecorder.recordIngest((System.nanoTime() - startedNanos) / 1_000_000_000.0, payloadBytes);
    }

    private void writeServerStatusOffline(List<Long> serverIds) {
        StringBuilder buffer = new StringBuilder();
        long epochSeconds = Instant.now().getEpochSecond();
        for (Long serverId : serverIds) {
            ServerStatusSeries.writeOffline(new MetricWriteContext(buffer, serverId, epochSeconds));
        }
        this.victoriaMetricsWriteClient.flush(buffer.toString());
    }

    private static ServerInventoryRow getOrCreateInventory(ServerRow server) {
        ServerInventoryRow inventory = server.getInventory();
        if (inventory == null) {
            inventory = new ServerInventoryRow(server);
            server.setInventory(inventory);
        }
        return inventory;
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
            return ServerResponse.from(server, role, null, null, null, null, null, uptime, folderName);
        }
        long serverId = server.getId();
        LatestHostMetrics metrics = context.hostMetrics();
        return ServerResponse.from(
                server,
                role,
                metrics.cpu().get(serverId),
                NumberUtils.toLong(metrics.memUsage().get(serverId)),
                NumberUtils.toLong(metrics.memMax().get(serverId)),
                NumberUtils.toLong(metrics.diskUsage().get(serverId)),
                NumberUtils.toLong(metrics.diskMax().get(serverId)),
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
                latest.getOrDefault(HostSeries.MEM_USAGE, Map.of()),
                latest.getOrDefault(HostSeries.MEM_TOTAL, Map.of()),
                latest.getOrDefault(DiskSeries.USED_BYTES, Map.of()),
                latest.getOrDefault(DiskSeries.TOTAL_BYTES, Map.of())
        );
    }

    private UUID issueIngestToken(long serverId) {
        UUID ingestToken = UUID.randomUUID();
        this.serverIngestTokenRepository.save(new IngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverId));
        return ingestToken;
    }

    private record ServerResponseContext(
            LatestHostMetrics hostMetrics,
            Map<Long, Double> uptimePercent30d,
            Map<Long, String> folderNamesByServerId
    ) {}

    private record LatestHostMetrics(
            Map<Long, Double> cpu,
            Map<Long, Double> memUsage,
            Map<Long, Double> memMax,
            Map<Long, Double> diskUsage,
            Map<Long, Double> diskMax
    ) {
        private static LatestHostMetrics empty() {
            return new LatestHostMetrics(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
