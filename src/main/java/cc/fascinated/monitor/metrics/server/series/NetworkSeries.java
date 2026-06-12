package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.InterfaceMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum NetworkSeries implements VmMetricFamily {
    RX_BPS("monitor_net_rx_bps"),
    TX_BPS("monitor_net_tx_bps"),
    RX_PACKETS_PER_SECOND("monitor_net_rx_packets_per_second"),
    TX_PACKETS_PER_SECOND("monitor_net_tx_packets_per_second"),
    RX_ERRORS_PER_SECOND("monitor_net_rx_errors_per_second"),
    TX_ERRORS_PER_SECOND("monitor_net_tx_errors_per_second");

    private final String metricName;

    NetworkSeries(String metricName) {
        this.metricName = metricName;
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
