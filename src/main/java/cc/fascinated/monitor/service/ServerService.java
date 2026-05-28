package cc.fascinated.monitor.service;

import cc.fascinated.monitor.exception.impl.InternalServerException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.request.server.ServerCreateRequest;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsArcMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsPoolMetric;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.model.persistance.*;
import cc.fascinated.monitor.model.persistance.metric.ServerDiskMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ServerIngestTokenRow;
import cc.fascinated.monitor.model.persistance.metric.ServerInterfaceMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ServerMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ServerZfsArcMetricRow;
import cc.fascinated.monitor.model.persistance.metric.ServerZfsPoolMetricRow;
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

    public ServerService(ServerRepository serverRepository, ServerIngestTokenRepository serverIngestTokenRepository,
                         ServerMetricsRepository serverMetricsRepository, ServerInterfaceMetricsRepository serverInterfaceMetricsRepository,
                         ServerDiskMetricsRepository serverDiskMetricsRepository,
                         ServerZfsArcMetricsRepository serverZfsArcMetricsRepository,
                         ServerZfsPoolMetricsRepository serverZfsPoolMetricsRepository) {
        this.serverRepository = serverRepository;
        this.serverIngestTokenRepository = serverIngestTokenRepository;
        this.serverMetricsRepository = serverMetricsRepository;
        this.serverInterfaceMetricsRepository = serverInterfaceMetricsRepository;
        this.serverDiskMetricsRepository = serverDiskMetricsRepository;
        this.serverZfsArcMetricsRepository = serverZfsArcMetricsRepository;
        this.serverZfsPoolMetricsRepository = serverZfsPoolMetricsRepository;
    }

    public CreatedServerResponse createServer(ServerCreateRequest createRequest) {
        try {
            ServerRow serverRow = this.serverRepository.save(new ServerRow(createRequest.name(), Instant.now()));
            UUID ingestToken = UUID.randomUUID();
            this.serverIngestTokenRepository.save(new ServerIngestTokenRow(AuthUtils.hash(ingestToken.toString()), serverRow.getId()));

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
        UUID token = AuthUtils.parseBearerToken(authorizationHeader);
        ServerIngestTokenRow tokenRow = this.serverIngestTokenRepository.findByTokenHash(AuthUtils.hash(token.toString()))
                .orElseThrow(() -> new UnauthorizedException("Invalid ingest token"));
        return findServerRowById(tokenRow.getServerId());
    }

    @Transactional
    public void ingestMetrics(ServerRow server, IngestServerMetrics metrics) {
        Instant now = Instant.now();
        
        // todo: verify agent version
        server.setAgentVersion(metrics.agentVersion());

        ServerDetails serverDetails = metrics.serverDetails();
        server.setIp(serverDetails.ip());
        server.setCoreCount(serverDetails.coreCount());
        server.setThreadCount(serverDetails.threadCount());
        server.setOsName(serverDetails.osName());
        server.setOsVersion(serverDetails.osVersion());
        server.setLastUptimeSeconds(serverDetails.uptimeSeconds());
        server.setCpuModel(serverDetails.cpuModel());
        server.setSocketCount(serverDetails.socketCount());
        server.setCpuClockMhz(serverDetails.cpuClockMhz());
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

        ZfsArcMetrics zfsArcMetrics = metrics.zfsArcMetrics();
        if (zfsArcMetrics != null) {
            this.serverZfsArcMetricsRepository.save(new ServerZfsArcMetricRow(
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
                this.serverZfsPoolMetricsRepository.save(new ServerZfsPoolMetricRow(
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

        for (InterfaceMetrics interfaceMetric : metrics.interfaceMetrics()) {
            this.serverInterfaceMetricsRepository.save(new ServerInterfaceMetricRow(
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
            this.serverDiskMetricsRepository.save(new ServerDiskMetricRow(
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

        this.serverRepository.save(server);
        log.info("Ingested metrics for {} in {}ms", server.getId(), System.currentTimeMillis() - now.toEpochMilli());
    }
}
