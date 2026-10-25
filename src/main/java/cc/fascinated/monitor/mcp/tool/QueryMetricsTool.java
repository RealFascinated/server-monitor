package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.mcp.McpMetricQueryService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.dto.McpMetricQueryResult;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QueryMetricsTool {
    private final McpAuth auth;
    private final McpMetricQueryService queryService;

    public QueryMetricsTool(McpAuth auth, McpMetricQueryService queryService) {
        this.auth = auth;
        this.queryService = queryService;
    }

    @McpTool(name = "query_metrics", description = """
            History of one metric on one server. Get the metric id, and the label values to filter on, from \
            list_metrics (with serverId). Returns unit and displayName (alertable metrics only), from/to and \
            stepSeconds, then one entry per series (per disk, interface, container, ...; at most 20, with \
            truncated=true when more matched, so filter with labels) holding min, max, avg, last and up to 200 \
            [epochSeconds, value] points one step apart. The statistics are taken over those points, so a spike \
            shorter than the step can be missed: narrow the range to find it. Read the statistics first and the \
            points only to see when something happened. Defaults to the last hour; the range can be at most 31 days \
            and the newest ~30 seconds may not be queryable yet. A series with no data in the range is left out.""")
    public String queryMetrics(
            McpTransportContext context,
            @McpToolParam(description = "Server id from list_servers") long serverId,
            @McpToolParam(description = "Metric id from list_metrics, e.g. monitor_host_cpu_usage") String metric,
            @McpToolParam(description = "Only series with these label values, e.g. {\"disk\":\"/\"}. Keys come from labelKeys and values from series in list_metrics with serverId", required = false)
            Map<String, String> labels,
            @McpToolParam(description = "Start, ISO-8601 instant like 2026-01-31T14:00:00Z, before 'to' (default: an hour before 'to')", required = false) String from,
            @McpToolParam(description = "End, ISO-8601 instant (default: now)", required = false) String to,
            @McpToolParam(description = "Seconds between points, at least 15; raised automatically so the range fits in 200 points (default: the smallest that fits)", required = false) Integer stepSeconds
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.METRICS_READ);
        return McpJson.write(this.queryService.query(user, serverId, metric, labels, from, to, stepSeconds));
    }
}
