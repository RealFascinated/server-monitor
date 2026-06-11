package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ServerRenameRequest;
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
import cc.fascinated.monitor.metrics.counter.IngestAuthFailuresCounterMetric;
import cc.fascinated.monitor.metrics.counter.TotalIngestsCounterMetric;
import cc.fascinated.monitor.metrics.histogram.IngestDurationHistogramMetric;
import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsWriteClient;
import cc.fascinated.monitor.metrics.vm.series.impl.CpuCoreSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DiskSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.DockerSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.GpuSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.HostSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ServerStatusSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TemperatureSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.NetworkSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsArcSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsPoolSeries;
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
    private final TotalIngestsCounterMetric totalIngestsCounterMetric;
    private final IngestDurationHistogramMetric ingestDurationHistogramMetric;
    private final IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final ServerMetricService serverMetricService;
    private final ServerAccessService serverAccessService;
    private final MonitorServerProperties serverProperties;
    private final ServerWebSocketService serverWebSocketService;

    public ServerService(ServerRepository serverRepository, ServerMemberRepository serverMemberRepository,
                         ServerIngestTokenRepository serverIngestTokenRepository,
                         TotalIngestsCounterMetric totalIngestsCounterMetric,
                         IngestDurationHistogramMetric ingestDurationHistogramMetric,
                         IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric,
                         VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                         ServerMetricService serverMetricService,
                         ServerAccessService serverAccessService,
                         MonitorServerProperties serverProperties,
                         @Lazy ServerWebSocketService serverWebSocketService) {
        this.serverRepository = serverRepository;
        this.serverMemberRepository = serverMemberRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.totalIngestsCounterMetric = totalIngestsCounterMetric;
        this.ingestDurationHistogramMetric = ingestDurationHistogramMetric;
        this.ingestAuthFailuresCounterMetric = ingestAuthFailuresCounterMetric;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.serverMetricService = serverMetricService;
        this.serverAccessService = serverAccessService;
        this.serverProperties = serverProperties;
        this.serverWebSocketService = serverWebSocketService;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    @Transactional
    public void markStaleServersOffline() {
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
        ServerResponseContext context = fetchServerResponseContext(servers);
        Map<Long, ServerRole> rolesByServerId = this.serverAccessService.findRolesByUserId(user.getId());
        return servers.stream()
                .map(server -> toServerResponse(server, rolesByServerId.get(server.getId()), context))
                .toList();
    }

    public ServerResponse getServer(UserRow user, long serverId) {
        ServerRow server = getAccessibleServer(user, serverId);
        ServerRole role = this.serverAccessService.findRole(server.getId(), user.getId()).orElseThrow();
        ServerResponseContext context = fetchServerResponseContext(List.of(server));
        return toServerResponse(server, role, context);
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
            this.ingestAuthFailuresCounterMetric.recordFailure();
            throw ex;
        }
    }

    @Transactional
    public void ingestMetrics(ServerRow server, IngestServerMetrics metrics) {
        long startedNanos = System.nanoTime();
        boolean success = false;
        try {
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

            this.totalIngestsCounterMetric.recordIngest();
            success = true;
        } finally {
            if (success) {
                this.ingestDurationHistogramMetric.observeSeconds((System.nanoTime() - startedNanos) / 1_000_000_000.0);
            }
        }
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

    private ServerResponseContext fetchServerResponseContext(List<ServerRow> servers) {
        List<Long> serverIds = servers.stream().map(ServerRow::getId).toList();
        List<Long> onlineServerIds = servers.stream()
                .filter(server -> server.getStatus() == ServerStatus.ONLINE)
                .map(ServerRow::getId)
                .toList();
        return new ServerResponseContext(
                fetchLatestHostMetrics(onlineServerIds),
                this.serverMetricService.fetchUptimePercent30d(serverIds)
        );
    }

    private ServerResponse toServerResponse(ServerRow server, ServerRole role, ServerResponseContext context) {
        Double uptime = context.uptimePercent30d().get(server.getId());
        if (server.getStatus() != ServerStatus.ONLINE) {
            return ServerResponse.from(server, role, null, null, null, null, null, uptime);
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
                uptime
        );
    }

    private LatestHostMetrics fetchLatestHostMetrics(List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return LatestHostMetrics.empty();
        }
        return new LatestHostMetrics(
                this.serverMetricService.fetchLatestMetric(HostSeries.CPU_USAGE, serverIds),
                this.serverMetricService.fetchLatestMetric(HostSeries.MEM_USAGE, serverIds),
                this.serverMetricService.fetchLatestMetric(HostSeries.MEM_TOTAL, serverIds),
                this.serverMetricService.fetchLatestMetric(DiskSeries.USED_BYTES, serverIds, ROOT_DISK_LABEL),
                this.serverMetricService.fetchLatestMetric(DiskSeries.TOTAL_BYTES, serverIds, ROOT_DISK_LABEL)
        );
    }

    private UUID issueIngestToken(long serverId) {
        UUID ingestToken = UUID.randomUUID();
        this.serverIngestTokenRepository.save(new IngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverId));
        return ingestToken;
    }

    private record ServerResponseContext(LatestHostMetrics hostMetrics, Map<Long, Double> uptimePercent30d) {}

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
