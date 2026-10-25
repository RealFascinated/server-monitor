package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertPreviewService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.McpMetricCatalogService;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.dto.McpAlertPreview;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.request.alert.AlertPreviewRequest;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PreviewAlertRuleTool {
    private static final int DEFAULT_RANGE_HOURS = 24;

    private final McpAuth auth;
    private final AlertPreviewService previewService;

    public PreviewAlertRuleTool(McpAuth auth, AlertPreviewService previewService) {
        this.auth = auth;
        this.previewService = previewService;
    }

    @McpTool(name = "preview_alert_rule", description = """
            Dry-runs a rule definition against one server's real history with the same engine live alerts use. \
            Nothing is created or sent. Use it before create_alert_rule or update_alert_rule to pick a threshold \
            that is not noisy; to check an existing rule, pass its values from list_alert_rules. Returns unit, \
            from/to (epoch seconds), stepSeconds (the replay resolution: range/480, at least 15, so shorter \
            spikes can be missed), timesFired across all series, and per series (at most 10, truncated=true when \
            more matched) its labels, the raw min/max/avg over the range, and firing: each interval it would have \
            fired, with from/to in epoch seconds, ongoing (still firing at the end) and peak (the worst compared \
            value, window-averaged when windowSeconds is set).""")
    public String previewAlertRule(
            McpTransportContext context,
            @McpToolParam(description = "Server whose history to replay, id from list_servers") long serverId,
            @McpToolParam(description = "Metric id from list_metrics with alertable=true") String metric,
            @McpToolParam(description = "MORE_THAN fires when the value goes above the threshold, LESS_THAN when it goes below") AlertOperator operator,
            @McpToolParam(description = "Threshold in the metric's unit (percent metrics are 0-100)") double threshold,
            @McpToolParam(description = "Only series with these label values, e.g. {\"disk\":\"/\"} (default: every series)", required = false) Map<String, String> labels,
            @McpToolParam(description = "Value the alert must get back past to resolve: for MORE_THAN at or below threshold, for LESS_THAN at or above it (default: the threshold)", required = false) Double recoveryThreshold,
            @McpToolParam(description = "Compare the rolling average over this many seconds: 0 = each sample, otherwise 10-86400 (default 0)", required = false) Integer windowSeconds,
            @McpToolParam(description = "Hours of history to replay, 1-168 (default 24)", required = false) Integer rangeHours
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_READ);
        McpMetricCatalogService.requireAlertable(metric);
        return McpJson.write(McpAlertPreview.from(this.previewService.preview(user, new AlertPreviewRequest(serverId, metric, labels,
                operator, threshold, recoveryThreshold, windowSeconds,
                rangeHours == null ? DEFAULT_RANGE_HOURS : rangeHours))));
    }
}
