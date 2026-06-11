package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
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
import cc.fascinated.monitor.metrics.vm.series.impl.TemperatureSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.NetworkSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsArcSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.vm.series.impl.ZfsPoolSeries;
import cc.fascinated.monitor.repository.ServerIngestTokenRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ServerService {
    private static final Comparator<String> SERVER_NAME_STRING_ORDER = (left, right) -> {
        boolean leftStartsWithLetter = startsWithLetter(left);
        boolean rightStartsWithLetter = startsWithLetter(right);
        if (leftStartsWithLetter != rightStartsWithLetter) {
            return leftStartsWithLetter ? 1 : -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    };

    private static final Comparator<ServerRow> SERVER_NAME_ORDER = Comparator.comparing(
            ServerRow::getServerName,
            SERVER_NAME_STRING_ORDER
    );

    private final ServerRepository serverRepository;
    private final ServerIngestTokenRepository serverIngestTokenRepository;
    private final TotalIngestsCounterMetric totalIngestsCounterMetric;
    private final IngestDurationHistogramMetric ingestDurationHistogramMetric;
    private final IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final ServerMetricService serverMetricService;
    private final ServerAccessService serverAccessService;
    private final MonitorServerProperties serverProperties;

    public ServerService(ServerRepository serverRepository, ServerIngestTokenRepository serverIngestTokenRepository,
                         TotalIngestsCounterMetric totalIngestsCounterMetric,
                         IngestDurationHistogramMetric ingestDurationHistogramMetric,
                         IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric,
                         VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                         ServerMetricService serverMetricService,
                         ServerAccessService serverAccessService,
                         MonitorServerProperties serverProperties) {
        this.serverRepository = serverRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.totalIngestsCounterMetric = totalIngestsCounterMetric;
        this.ingestDurationHistogramMetric = ingestDurationHistogramMetric;
        this.ingestAuthFailuresCounterMetric = ingestAuthFailuresCounterMetric;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.serverMetricService = serverMetricService;
        this.serverAccessService = serverAccessService;
        this.serverProperties = serverProperties;
    }

    public List<ServerResponse> listServers(UserRow user) {
        List<ServerRow> servers = listAccessibleServers(user);
        if (servers.isEmpty()) {
            return List.of();
        }
        List<Long> onlineServerIds = servers.stream()
                .filter(server -> server.getStatus() == ServerStatus.ONLINE)
                .map(ServerRow::getId)
                .toList();
        final Map<Long, Double> cpuByServer;
        final Map<Long, Double> memUsageByServer;
        final Map<Long, Double> memMaxByServer;
        if (onlineServerIds.isEmpty()) {
            cpuByServer = Map.of();
            memUsageByServer = Map.of();
            memMaxByServer = Map.of();
        } else {
            cpuByServer = this.serverMetricService.fetchLatestMetric(HostSeries.CPU_USAGE, onlineServerIds);
            memUsageByServer = this.serverMetricService.fetchLatestMetric(HostSeries.MEM_USAGE, onlineServerIds);
            memMaxByServer = this.serverMetricService.fetchLatestMetric(HostSeries.MEM_TOTAL, onlineServerIds);
        }
        return servers.stream()
                .map(server -> {
                    if (server.getStatus() != ServerStatus.ONLINE) {
                        return ServerResponse.from(server, null, null, null);
                    }
                    return ServerResponse.from(
                            server,
                            cpuByServer.get(server.getId()),
                            toLong(memUsageByServer.get(server.getId())),
                            toLong(memMaxByServer.get(server.getId()))
                    );
                })
                .toList();
    }

    public CreatedServerResponse createServer(UserRow user, ServerCreateRequest createRequest) {
        try {
            ServerRow serverRow = this.serverRepository.save(new ServerRow(createRequest.name(), user.getId(), Instant.now()));
            UUID ingestToken = issueIngestToken(serverRow.getId());

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

    public ServerRow getOwnedServer(UserRow user, long serverId) {
        ServerRow server = findServerRowById(serverId);
        requireServerOwner(user, server);
        return server;
    }

    public ServerRow getAccessibleServer(UserRow user, long serverId) {
        ServerRow server = findServerRowById(serverId);
        if (server.getOwnerId().equals(user.getId())) {
            return server;
        }
        if (this.serverAccessService.isMember(serverId, user.getId())) {
            return server;
        }
        throw new UnauthorizedException("You do not have access to this server");
    }

    @Transactional
    public ServerResponse renameServer(UserRow user, long serverId, ServerRenameRequest request) {
        ServerRow server = getOwnedServer(user, serverId);
        server.setServerName(request.name());
        this.serverRepository.save(server);
        return ServerResponse.from(server, null, null, null);
    }

    @Transactional
    public void deleteServer(UserRow user, long serverId) {
        requireServerOwner(user, findServerRowById(serverId));
        this.victoriaMetricsWriteClient.deleteSeriesForServer(serverId);
        this.serverRepository.deleteById(serverId);
        log.info("Deleted server {} and its VictoriaMetrics series", serverId);
    }

    @Transactional
    public IngestTokenResponse rotateIngestToken(UserRow user, long serverId) {
        requireServerOwner(user, findServerRowById(serverId));
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
            this.victoriaMetricsWriteClient.flush(buffer.toString());

            this.serverRepository.save(server);

            this.totalIngestsCounterMetric.recordIngest();
            success = true;
        } finally {
            if (success) {
                this.ingestDurationHistogramMetric.observeSeconds((System.nanoTime() - startedNanos) / 1_000_000_000.0);
            }
        }
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    @Transactional
    public void markStaleServersOffline() {
        Instant cutoff = Instant.now().minus(this.serverProperties.getOfflineThreshold());
        int updated = this.serverRepository.markStaleServersOffline(cutoff);
        if (updated > 0) {
            log.info("Marked {} server(s) offline (no update since {})", updated, cutoff);
        }
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
        List<ServerRow> owned = this.serverRepository.findByOwnerId(user.getId());
        List<Long> memberServerIds = this.serverAccessService.findMemberServerIds(user.getId());
        if (memberServerIds.isEmpty()) {
            owned.sort(SERVER_NAME_ORDER);
            return owned;
        }

        Set<Long> ownedIds = new LinkedHashSet<>();
        for (ServerRow server : owned) {
            ownedIds.add(server.getId());
        }

        List<ServerRow> combined = new ArrayList<>(owned);
        for (ServerRow server : this.serverRepository.findAllById(memberServerIds)) {
            if (!ownedIds.contains(server.getId())) {
                combined.add(server);
            }
        }
        combined.sort(SERVER_NAME_ORDER);
        return combined;
    }

    private static boolean startsWithLetter(String name) {
        return !name.isEmpty() && Character.isLetter(name.charAt(0));
    }

    private static void requireServerOwner(UserRow user, ServerRow server) {
        if (!server.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this server");
        }
    }

    private UUID issueIngestToken(long serverId) {
        UUID ingestToken = UUID.randomUUID();
        this.serverIngestTokenRepository.save(new IngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverId));
        return ingestToken;
    }

    private static Long toLong(Double value) {
        return value != null ? value.longValue() : null;
    }
}
