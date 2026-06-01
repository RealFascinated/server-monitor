package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorServerProperties;
import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.dto.response.server.IngestTokenResponse;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ServerService {
    private final ServerRepository serverRepository;
    private final ServerIngestTokenRepository serverIngestTokenRepository;
    private final TotalIngestsCounterMetric totalIngestsCounterMetric;
    private final IngestDurationHistogramMetric ingestDurationHistogramMetric;
    private final IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final MonitorServerProperties serverProperties;

    public ServerService(ServerRepository serverRepository, ServerIngestTokenRepository serverIngestTokenRepository,
                         TotalIngestsCounterMetric totalIngestsCounterMetric,
                         IngestDurationHistogramMetric ingestDurationHistogramMetric,
                         IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric,
                         VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                         MonitorServerProperties serverProperties) {
        this.serverRepository = serverRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.totalIngestsCounterMetric = totalIngestsCounterMetric;
        this.ingestDurationHistogramMetric = ingestDurationHistogramMetric;
        this.ingestAuthFailuresCounterMetric = ingestAuthFailuresCounterMetric;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.serverProperties = serverProperties;
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
}
