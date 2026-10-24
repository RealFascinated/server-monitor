package cc.fascinated.monitor.server.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import cc.fascinated.monitor.model.dto.request.server.ingest.IngestServerMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.CpuCoreMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DockerContainerMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.GpuMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.SmartMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TcpConnectionMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TemperatureMetric;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsPoolMetric;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Structural sanity checks for agent payloads. Agents are authenticated but not trusted:
 * names become metric label values and list lengths become series counts.
 */
@Component
public class IngestSampleChecker {
    public static final String REASON_INVALID = "invalid";
    public static final String REASON_OVERSIZED = "oversized";

    private final MonitorIngestProperties properties;

    public IngestSampleChecker(MonitorIngestProperties properties) {
        this.properties = properties;
    }

    /**
     * @return {@code null} when the sample is acceptable, otherwise the reason it was refused
     */
    public String rejectReason(IngestServerMetrics sample) {
        if (sample == null || sample.serverDetails() == null || sample.serverMetrics() == null
                || sample.agentVersion() == null || sample.agentVersion().isBlank()) {
            return REASON_INVALID;
        }

        var serverMetrics = sample.serverMetrics();
        int limit = this.properties.getMaxListItems();
        if (tooLong(sample.interfaceMetrics(), limit) || tooLong(sample.diskMetrics(), limit)
                || tooLong(sample.gpuMetrics(), limit) || tooLong(sample.zfsPoolMetrics(), limit)
                || tooLong(sample.dockerContainers(), limit) || tooLong(sample.tcpConnectionMetrics(), limit)
                || tooLong(sample.smartMetrics(), limit)
                || tooLong(serverMetrics.cpuCoreMetrics(), limit) || tooLong(serverMetrics.temperatureMetrics(), limit)) {
            return REASON_OVERSIZED;
        }

        if (!labelsValid(sample.interfaceMetrics(), InterfaceMetrics::interfaceName)
                || !labelsValid(sample.diskMetrics(), DiskMetric::diskName)
                || !labelsValid(sample.gpuMetrics(), GpuMetric::deviceId)
                || !labelsValid(sample.gpuMetrics(), GpuMetric::name)
                || !labelsValid(sample.gpuMetrics(), GpuMetric::vendor)
                || !labelsValid(sample.zfsPoolMetrics(), ZfsPoolMetric::poolName)
                || !labelsValid(sample.zfsPoolMetrics(), ZfsPoolMetric::health)
                || !labelsValid(sample.zfsPoolMetrics(), ZfsPoolMetric::scanState)
                || !labelsValid(sample.dockerContainers(), DockerContainerMetric::containerName)
                || !labelsValid(sample.tcpConnectionMetrics(), TcpConnectionMetric::state)
                || !labelsValid(sample.smartMetrics(), SmartMetric::device)
                || !labelsValid(sample.smartMetrics(), SmartMetric::model)
                || !labelsValid(serverMetrics.cpuCoreMetrics(), CpuCoreMetric::cpu)
                || !labelsValid(serverMetrics.temperatureMetrics(), TemperatureMetric::sensor)) {
            return REASON_INVALID;
        }
        return null;
    }

    private static boolean tooLong(List<?> items, int limit) {
        return items != null && items.size() > limit;
    }

    private <T> boolean labelsValid(List<T> items, Function<T, String> label) {
        if (items == null) {
            return true;
        }
        for (T item : items) {
            if (item == null || !labelValid(label.apply(item))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Label values are written into the Prometheus text format, where a newline would start a
     * new series line, so control characters are refused outright.
     */
    private boolean labelValid(String value) {
        if (value == null || value.length() > this.properties.getMaxLabelLength()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
