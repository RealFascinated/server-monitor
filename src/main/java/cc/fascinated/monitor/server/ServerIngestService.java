package cc.fascinated.monitor.server;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import cc.fascinated.monitor.metrics.server.series.CpuCoreSeries;
import cc.fascinated.monitor.metrics.server.series.DiskSeries;
import cc.fascinated.monitor.metrics.server.series.DockerSeries;
import cc.fascinated.monitor.metrics.server.series.GpuSeries;
import cc.fascinated.monitor.metrics.server.series.HostSeries;
import cc.fascinated.monitor.metrics.server.series.NetworkSeries;
import cc.fascinated.monitor.metrics.server.series.ServerStatusSeries;
import cc.fascinated.monitor.metrics.server.series.TcpConnectionSeries;
import cc.fascinated.monitor.metrics.server.series.TemperatureSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsArcSeries;
import cc.fascinated.monitor.metrics.server.series.ZfsPoolSeries;
import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsWriteClient;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.persistance.ServerInventoryRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.metric.IngestTokenRow;
import cc.fascinated.monitor.repository.ServerIngestTokenRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.util.AuthUtils;
import cc.fascinated.monitor.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ServerIngestService {
    private final ServerRepository serverRepository;
    private final ServerIngestTokenRepository serverIngestTokenRepository;
    private final PlatformMetricsRecorder platformMetricsRecorder;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;
    private final IncidentService incidentService;

    public ServerIngestService(ServerRepository serverRepository,
                               ServerIngestTokenRepository serverIngestTokenRepository,
                               PlatformMetricsRecorder platformMetricsRecorder,
                               VictoriaMetricsWriteClient victoriaMetricsWriteClient,
                               IncidentService incidentService) {
        this.serverRepository = serverRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.platformMetricsRecorder = platformMetricsRecorder;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
        this.incidentService = incidentService;
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
        ServerStatus previousStatus = server.getStatus();
        server.setLastHeartbeat(now);
        if (server.getStatus() != ServerStatus.ONLINE) {
            server.setStatus(ServerStatus.ONLINE);
        }
        this.serverRepository.save(server);
        if (previousStatus == ServerStatus.OFFLINE) {
            this.incidentService.resolveOpenOutage(server.getId(), now);
        }
    }

    @Transactional
    public void ingestMetrics(ServerRow server, IngestServerMetrics metrics, long payloadBytes) {
        long startedNanos = System.nanoTime();

        Instant now = Instant.now();
        ServerStatus previousStatus = server.getStatus();

        String agentVersion = metrics.agentVersion();
        if (agentVersion == null || agentVersion.isBlank()) {
            throw new BadRequestException("agent version is required");
        }
        server.setAgentVersion(agentVersion.trim());

        ServerDetails serverDetails = metrics.serverDetails();
        ServerInventoryRow inventory = getOrCreateInventory(server);
        inventory.setIp(serverDetails.ip());
        inventory.setCoreCount(serverDetails.coreCount());
        inventory.setThreadCount(serverDetails.threadCount());
        inventory.setOsName(serverDetails.osName());
        inventory.setOsVersion(serverDetails.osVersion());
        inventory.setKernelVersion(serverDetails.kernelVersion());
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

        if (previousStatus == ServerStatus.OFFLINE) {
            this.incidentService.resolveOpenOutage(server.getId(), now);
        }

        this.platformMetricsRecorder.recordIngest((System.nanoTime() - startedNanos) / 1_000_000_000.0, payloadBytes);
    }

    public void writeServerStatusOffline(List<Long> serverIds) {
        StringBuilder buffer = new StringBuilder();
        long epochSeconds = Instant.now().getEpochSecond();
        for (Long serverId : serverIds) {
            ServerStatusSeries.writeOffline(new MetricWriteContext(buffer, serverId, epochSeconds));
        }
        this.victoriaMetricsWriteClient.flush(buffer.toString());
    }

    @Transactional
    public UUID issueIngestToken(long serverId) {
        UUID ingestToken = UUID.randomUUID();
        this.serverIngestTokenRepository.save(new IngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverId));
        return ingestToken;
    }

    @Transactional
    public UUID rotateIngestToken(long serverId) {
        UUID ingestToken = UUID.randomUUID();
        String tokenHash = AuthUtils.hash(ingestToken.toString());
        IngestTokenRow row = this.serverIngestTokenRepository.findByServerId(serverId)
                .map(existing -> {
                    existing.setTokenHash(tokenHash);
                    return existing;
                })
                .orElseGet(() -> new IngestTokenRow(tokenHash, serverId));
        this.serverIngestTokenRepository.save(row);
        log.info("Rotated ingest token for server {}", serverId);
        return ingestToken;
    }

    private ServerRow findServerRowById(long id) {
        Optional<ServerRow> serverRowOptional = this.serverRepository.findById(id);
        if (serverRowOptional.isPresent()) {
            return serverRowOptional.get();
        }
        throw new NotFoundException("Server \"%s\" not found".formatted(id));
    }

    private static ServerInventoryRow getOrCreateInventory(ServerRow server) {
        ServerInventoryRow inventory = server.getInventory();
        if (inventory == null) {
            inventory = new ServerInventoryRow(server);
            server.setInventory(inventory);
        }
        return inventory;
    }
}
