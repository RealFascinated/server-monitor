package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.mcp.McpMetricCatalogService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.auth.RequiresScope;
import cc.fascinated.monitor.mcp.dto.McpMetricInfo;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListMetricsTool {
    private final McpAuth auth;
    private final McpMetricCatalogService catalog;

    public ListMetricsTool(McpAuth auth, McpMetricCatalogService catalog) {
        this.auth = auth;
        this.catalog = catalog;
    }

    @RequiresScope(ApiTokenScope.METRICS_READ)
    @McpTool(name = "list_metrics", description = """
            The only metric discovery tool: lists the metric ids that query_metrics accepts and the alert tools use. \
            Each entry has its group, labelKeys (the labels that tell its series apart, e.g. disk, interface, \
            container; empty for one-series metrics) and alertable. Alertable metrics also carry displayName, unit \
            (PERCENT 0-100, BYTES, BYTES_PER_SECOND, MILLISECONDS, TEMPERATURE in Celsius, WATTS, COUNT, or STATE \
            where 1 = healthy and 0 = unhealthy) and the suggestedOperator/suggestedThreshold the dashboard \
            proposes. Other metrics have no unit; read it from the id suffix (_bytes, _bps = bytes/s, _pct or \
            _percent = 0-100, _ms, _celsius, _watts, _per_second, _total = counter). Without serverId this is the \
            whole catalog. With serverId it lists only what that server reported in the last few minutes. Add includeSeries=true for series: the label values that exist (which disks, \
            interfaces, containers, GPUs), to use as labels in query_metrics and alert rules. That is a lot of \
            output for a whole server, so pair it with group or metrics.""")
    public String listMetrics(
            McpTransportContext context,
            @McpToolParam(description = "Server id from list_servers: only metrics it reports", required = false) Long serverId,
            @McpToolParam(description = "Only one group: host, psi, cpuCores, disks, networks, gpus, containers, temperatures, zfsArc, zfsPools, tcpConnections or smartDevices", required = false) String group,
            @McpToolParam(description = "Only these metric ids, e.g. [\"monitor_host_cpu_usage\", \"monitor_disk_usage_percent\"]", required = false) List<String> metrics,
            @McpToolParam(description = "With serverId: include each metric's label values as series (default false; large unless narrowed by group or metrics)", required = false) Boolean includeSeries
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.METRICS_READ);
        return McpJson.write(this.catalog.list(user, group, metrics, serverId, Boolean.TRUE.equals(includeSeries)));
    }
}
