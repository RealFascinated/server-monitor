package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public enum NetworkSeries implements VmMetricFamily {
    RX_BPS("monitor_net_rx_bps",
            AlertInfo.of("Network receive rate", AlertUnit.BYTES_PER_SECOND, AlertOperator.MORE_THAN, 104857600)),
    TX_BPS("monitor_net_tx_bps",
            AlertInfo.of("Network transmit rate", AlertUnit.BYTES_PER_SECOND, AlertOperator.MORE_THAN, 104857600)),
    RX_PACKETS_PER_SECOND("monitor_net_rx_packets_per_second"),
    TX_PACKETS_PER_SECOND("monitor_net_tx_packets_per_second"),
    RX_ERRORS_PER_SECOND("monitor_net_rx_errors_per_second",
            AlertInfo.of("Network receive errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0)),
    TX_ERRORS_PER_SECOND("monitor_net_tx_errors_per_second",
            AlertInfo.of("Network transmit errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    NetworkSeries(String metricName) {
        this(metricName, null);
    }

    NetworkSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.NETWORK;
    }

    @Override
    public String metricPrefix() {
        return "monitor_net_";
    }

    public static void write(MetricWriteContext ctx, InterfaceMetrics iface) {
        MetricWriteContext labeled = ctx.withLabel("interface", iface.interfaceName());
        RX_BPS.writeNullable(labeled, iface.rxBytesPerSecond());
        TX_BPS.writeNullable(labeled, iface.txBytesPerSecond());
        RX_PACKETS_PER_SECOND.writeNullable(labeled, iface.rxPacketsPerSecond());
        TX_PACKETS_PER_SECOND.writeNullable(labeled, iface.txPacketsPerSecond());
        RX_ERRORS_PER_SECOND.writeNullable(labeled, iface.rxErrorsPerSecond());
        TX_ERRORS_PER_SECOND.writeNullable(labeled, iface.txErrorsPerSecond());
    }
}
