package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DockerContainerMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsArcMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsPoolMetric;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.persistance.*;
import cc.fascinated.monitor.model.persistance.metric.DiskMetricRow;
import cc.fascinated.monitor.model.persistance.metric.DockerContainerMetricRow;
import cc.fascinated.monitor.model.persistance.metric.IngestTokenRow;
import cc.fascinated.monitor.model.persistance.metric.InterfaceMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ServerMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ZfsArcMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ZfsPoolMetricRow;
import cc.fascinated.monitor.metrics.counter.IngestMetricRowsCounterMetric;
import cc.fascinated.monitor.metrics.counter.IngestAuthFailuresCounterMetric;
import cc.fascinated.monitor.metrics.counter.TotalIngestsCounterMetric;
import cc.fascinated.monitor.metrics.histogram.IngestDurationHistogramMetric;
import cc.fascinated.monitor.repository.*;
import cc.fascinated.monitor.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
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
    private final ServerMetricsRepository serverMetricsRepository;
    private final ServerInterfaceMetricsRepository serverInterfaceMetricsRepository;
    private final ServerDiskMetricsRepository serverDiskMetricsRepository;
    private final ServerZfsArcMetricsRepository serverZfsArcMetricsRepository;
    private final ServerZfsPoolMetricsRepository serverZfsPoolMetricsRepository;
    private final ServerDockerContainerMetricsRepository serverDockerContainerMetricsRepository;
    private final TotalIngestsCounterMetric totalIngestsCounterMetric;
    private final IngestMetricRowsCounterMetric ingestMetricRowsCounterMetric;
    private final IngestDurationHistogramMetric ingestDurationHistogramMetric;
    private final IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric;

    public ServerService(ServerRepository serverRepository, ServerIngestTokenRepository serverIngestTokenRepository,
                         ServerMetricsRepository serverMetricsRepository, ServerInterfaceMetricsRepository serverInterfaceMetricsRepository,
                         ServerDiskMetricsRepository serverDiskMetricsRepository,
                         ServerZfsArcMetricsRepository serverZfsArcMetricsRepository,
                         ServerZfsPoolMetricsRepository serverZfsPoolMetricsRepository,
                         ServerDockerContainerMetricsRepository serverDockerContainerMetricsRepository,
                         TotalIngestsCounterMetric totalIngestsCounterMetric,
                         IngestMetricRowsCounterMetric ingestMetricRowsCounterMetric,
                         IngestDurationHistogramMetric ingestDurationHistogramMetric,
                         IngestAuthFailuresCounterMetric ingestAuthFailuresCounterMetric) {
        this.serverRepository = serverRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.serverMetricsRepository = serverMetricsRepository;
        this.serverInterfaceMetricsRepository = serverInterfaceMetricsRepository;
        this.serverDiskMetricsRepository = serverDiskMetricsRepository;
        this.serverZfsArcMetricsRepository = serverZfsArcMetricsRepository;
        this.serverZfsPoolMetricsRepository = serverZfsPoolMetricsRepository;
        this.serverDockerContainerMetricsRepository = serverDockerContainerMetricsRepository;
        this.totalIngestsCounterMetric = totalIngestsCounterMetric;
        this.ingestMetricRowsCounterMetric = ingestMetricRowsCounterMetric;
        this.ingestDurationHistogramMetric = ingestDurationHistogramMetric;
        this.ingestAuthFailuresCounterMetric = ingestAuthFailuresCounterMetric;
    }

    public CreatedServerResponse createServer(UserRow user, ServerCreateRequest createRequest) {
        try {
            ServerRow serverRow = this.serverRepository.save(new ServerRow(createRequest.name(), user.getId(), Instant.now()));
            UUID ingestToken = UUID.randomUUID();
            this.serverIngestTokenRepository.save(new IngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverRow.getId()));

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
            inventory.setCpuClockMhz(serverDetails.cpuClockMhz());
            server.setLastUptimeSeconds(serverDetails.uptimeSeconds());
            server.setLastUpdated(now);
            server.setStatus(ServerStatus.ONLINE);

            ServerMetrics serverMetrics = metrics.serverMetrics();
            this.serverMetricsRepository.save(new ServerMetricRow(
                    server.getId(),
                    serverMetrics.cpuUsage(),
                    serverMetrics.memoryUsage(),
                    serverMetrics.memoryAvailable(),
                    serverMetrics.memoryTotal(),
                    serverMetrics.load1(),
                    serverMetrics.load5(),
                    serverMetrics.load15(),
                    serverMetrics.cpuUserPercent(),
                    serverMetrics.cpuSystemPercent(),
                    serverMetrics.cpuIowaitPercent(),
                    serverMetrics.cpuStealPercent(),
                    serverMetrics.memoryBuffers(),
                    serverMetrics.memoryCached(),
                    serverMetrics.swapUsed(),
                    serverMetrics.swapTotal(),
                    serverMetrics.processCount(),
                    serverMetrics.runningProcesses(),
                    serverMetrics.contextSwitchesPerSecond(),
                    serverMetrics.interruptsPerSecond(),
                    now
            ));

            for (InterfaceMetrics interfaceMetric : metrics.interfaceMetrics()) {
                this.serverInterfaceMetricsRepository.save(new InterfaceMetricRow(
                        server.getId(),
                        interfaceMetric.interfaceName(),
                        interfaceMetric.rxBytesPerSecond(),
                        interfaceMetric.txBytesPerSecond(),
                        interfaceMetric.rxPacketsPerSecond(),
                        interfaceMetric.txPacketsPerSecond(),
                        interfaceMetric.rxErrorsPerSecond(),
                        interfaceMetric.txErrorsPerSecond(),
                        now
                ));
            }

            for (DiskMetric diskMetric : metrics.diskMetrics()) {
                this.serverDiskMetricsRepository.save(new DiskMetricRow(
                        server.getId(),
                        diskMetric.diskName(),
                        ((double) diskMetric.usedBytes() / diskMetric.totalBytes()) * 100,
                        diskMetric.usedBytes(),
                        diskMetric.totalBytes(),
                        diskMetric.ioReadBytesPerSecond(),
                        diskMetric.ioWriteBytesPerSecond(),
                        diskMetric.ioUsagePercent(),
                        diskMetric.ioWaitMilliseconds(),
                        diskMetric.inodeUsed(),
                        diskMetric.inodeTotal(),
                        diskMetric.readIops(),
                        diskMetric.writeIops(),
                        diskMetric.readLatencyMs(),
                        diskMetric.writeLatencyMs(),
                        now
                ));
            }

            ZfsArcMetrics zfsArcMetrics = metrics.zfsArcMetrics();
            if (zfsArcMetrics != null) {
                this.serverZfsArcMetricsRepository.save(new ZfsArcMetricRow(
                        server.getId(),
                        zfsArcMetrics.arcSizeBytes(),
                        zfsArcMetrics.arcTargetBytes(),
                        zfsArcMetrics.arcMaxBytes(),
                        zfsArcMetrics.arcMinBytes(),
                        zfsArcMetrics.arcDataBytes(),
                        zfsArcMetrics.arcMetadataBytes(),
                        zfsArcMetrics.l2arcSizeBytes(),
                        zfsArcMetrics.arcHitRatio(),
                        zfsArcMetrics.arcMissesPerSecond(),
                        now
                ));
            }

            if (metrics.zfsPoolMetrics() != null) {
                for (ZfsPoolMetric poolMetric : metrics.zfsPoolMetrics()) {
                    this.serverZfsPoolMetricsRepository.save(new ZfsPoolMetricRow(
                            server.getId(),
                            poolMetric.poolName(),
                            poolMetric.health(),
                            poolMetric.capacityPercent(),
                            poolMetric.allocatedBytes(),
                            poolMetric.freeBytes(),
                            poolMetric.totalBytes(),
                            poolMetric.fragmentationPercent(),
                            poolMetric.scanState(),
                            poolMetric.scanPercent(),
                            poolMetric.readBps(),
                            poolMetric.writeBps(),
                            poolMetric.readIops(),
                            poolMetric.writeIops(),
                            poolMetric.checksumErrors(),
                            now
                    ));
                }
            }

            if (metrics.dockerContainers() != null) {
                for (DockerContainerMetric containerMetric : metrics.dockerContainers()) {
                    this.serverDockerContainerMetricsRepository.save(new DockerContainerMetricRow(
                            server.getId(),
                            containerMetric.containerName(),
                            containerMetric.cpuUsage().doubleValue(),
                            containerMetric.memoryUsage(),
                            now
                    ));
                }
            }

            this.serverRepository.save(server);

            this.totalIngestsCounterMetric.recordIngest();
            this.ingestMetricRowsCounterMetric.recordRows("server_metrics", 1);
            this.ingestMetricRowsCounterMetric.recordRows("server_network_metrics", metrics.interfaceMetrics().size());
            this.ingestMetricRowsCounterMetric.recordRows("server_disk_metrics", metrics.diskMetrics().size());
            if (metrics.zfsArcMetrics() != null) {
                this.ingestMetricRowsCounterMetric.recordRows("server_zfs_arc_metrics", 1);
            }
            if (metrics.zfsPoolMetrics() != null) {
                this.ingestMetricRowsCounterMetric.recordRows("server_zfs_pool_metrics", metrics.zfsPoolMetrics().size());
            }
            if (metrics.dockerContainers() != null) {
                this.ingestMetricRowsCounterMetric.recordRows("server_docker_container_metrics", metrics.dockerContainers().size());
            }
            success = true;
        } finally {
            if (success) {
                this.ingestDurationHistogramMetric.observeSeconds((System.nanoTime() - startedNanos) / 1_000_000_000.0);
            }
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
}
