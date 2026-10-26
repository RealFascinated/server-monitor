package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpToolsIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password-1234";
    private static final String CPU = "monitor_host_cpu_usage";

    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;
    private McpTestSupport mcp;
    private String session;
    private String all;
    private CreatedServerResponse server;
    private String other;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.mcp = new McpTestSupport(this.mockMvc, this.objectMapper);
        this.session = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();
        this.all = this.mcp.createToken(this.session, "servers:read", "metrics:read", "alerts:write");
        this.server = this.auth.createServer(this.session, "alpha");
        String otherSession = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();
        this.other = this.mcp.createToken(otherSession, "servers:read", "metrics:read", "alerts:write");
    }

    private void ingest(String payload) throws Exception {
        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(this.server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    /**
     * Writes samples a couple of minutes in the past. VictoriaMetrics hides the newest 30 seconds from queries, so
     * data ingested "now" would not be visible to the tools yet.
     */
    private void backfill(boolean withSmart) throws Exception {
        long now = java.time.Instant.now().getEpochSecond();
        StringBuilder samples = new StringBuilder();
        for (long offset : new long[]{180, 150, 120}) {
            if (samples.length() > 0) {
                samples.append(',');
            }
            String payload = withSmart ? TestFixtures.ingestPayloadWithSmart() : TestFixtures.minimalIngestPayload();
            samples.append(payload.replaceFirst("\"agentVersion\": \"1.0.0\",",
                    "\"agentVersion\": \"1.0.0\", \"timestamp\": " + (now - offset) + ","));
        }
        this.mockMvc.perform(post("/v1/servers/ingest/batch")
                        .header("Authorization", TestAuthSupport.bearer(this.server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentAt\": " + now + ", \"samples\": [" + samples + "]}"))
                .andExpect(status().isOk());
    }

    private JsonNode json(McpTestSupport.ToolResult result) {
        assertThat(result.isError()).as(result.text()).isFalse();
        return this.objectMapper.readTree(result.text());
    }

    private McpTestSupport.ToolResult ok(String token, String tool, Map<String, Object> args) throws Exception {
        McpTestSupport.ToolResult result = this.mcp.call(token, tool, args);
        assertThat(result.isError()).as("%s: %s", tool, result.text()).isFalse();
        return result;
    }

    private McpTestSupport.ToolResult awaitResult(String tool, Map<String, Object> args,
                                                  Predicate<McpTestSupport.ToolResult> done) throws Exception {
        McpTestSupport.ToolResult result = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            result = this.mcp.call(this.all, tool, args);
            if (!result.isError() && done.test(result)) {
                return result;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for " + tool + ", last result: " + (result == null ? null : result.text()));
    }

    private Map<String, Object> args(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void listServersOnlyReturnsTheTokensUsersServers() throws Exception {
        assertThat(json(ok(this.all, "list_servers", Map.of())).toString()).contains("alpha");
        assertThat(json(ok(this.other, "list_servers", Map.of())).size()).isZero();
    }

    @Test
    void listServersCanFilterToOneServerAndDeniesOtherUsers() throws Exception {
        JsonNode one = json(ok(this.all, "list_servers", args("serverId", this.server.serverId())));
        assertThat(one.size()).isEqualTo(1);
        assertThat(one.get(0).get("serverName").asString()).isEqualTo("alpha");

        McpTestSupport.ToolResult denied = this.mcp.call(this.other, "list_servers", args("serverId", this.server.serverId()));
        assertThat(denied.isError()).isTrue();
        assertThat(denied.text()).doesNotContain("alpha");
    }

    @Test
    void listServersCanBeCompactAndFilteredByName() throws Exception {
        JsonNode compact = json(ok(this.all, "list_servers", args("compact", true, "nameContains", "ALP")));
        assertThat(compact.size()).isEqualTo(1);
        assertThat(compact.get(0).get("serverName").asString()).isEqualTo("alpha");
        assertThat(compact.get(0).has("inventory")).isFalse();
        assertThat(compact.get(0).has("serverId")).isTrue();

        assertThat(json(ok(this.all, "list_servers", args("nameContains", "nope"))).size()).isZero();
    }

    @Test
    void listIncidentsReturnsAPageAndDeniesOtherUsers() throws Exception {
        JsonNode page = json(ok(this.all, "list_server_incidents", args("serverId", this.server.serverId())));
        assertThat(page.get("totalItems").asLong()).isZero();

        assertThat(this.mcp.call(this.other, "list_server_incidents", args("serverId", this.server.serverId())).isError())
                .isTrue();
    }

    @Test
    void listMetricsCoversEveryQueryableMetricNotJustAlertableOnes() throws Exception {
        JsonNode metrics = json(ok(this.all, "list_metrics", Map.of()));

        Map<String, JsonNode> byId = new HashMap<>();
        metrics.forEach(metric -> byId.put(metric.get("metric").asString(), metric));
        assertThat(byId.get(CPU).get("alertable").asBoolean()).isTrue();
        assertThat(byId.get(CPU).get("displayName").asString()).isNotBlank();
        assertThat(byId).containsKey("monitor_server_up");
        JsonNode powerOnHours = byId.get("monitor_smart_power_on_hours");
        assertThat(powerOnHours.get("alertable").asBoolean()).isFalse();
        assertThat(powerOnHours.get("group").asString()).isEqualTo("smartDevices");
        assertThat(powerOnHours.get("labelKeys").get(0).asString()).isEqualTo("device");
        assertThat(powerOnHours.has("series")).isFalse();
    }

    @Test
    void listMetricsFiltersByGroupAndRejectsUnknownGroups() throws Exception {
        JsonNode smart = json(ok(this.all, "list_metrics", args("group", "smartDevices")));
        assertThat(smart.size()).isGreaterThan(0);
        smart.forEach(metric -> assertThat(metric.get("group").asString()).isEqualTo("smartDevices"));

        McpTestSupport.ToolResult unknown = this.mcp.call(this.all, "list_metrics", args("group", "nope"));
        assertThat(unknown.isError()).isTrue();
        assertThat(unknown.text()).contains("Unknown group").contains("smartDevices");
    }

    @Test
    void listMetricsForAServerOnlyListsWhatItReportsWithItsLabelValues() throws Exception {
        backfill(true);

        McpTestSupport.ToolResult unnarrowed = awaitResult("list_metrics", args("serverId", this.server.serverId()),
                r -> r.text().contains("monitor_smart_data_written_bytes"));
        Map<String, JsonNode> unnarrowedById = new HashMap<>();
        this.objectMapper.readTree(unnarrowed.text())
                .forEach(metric -> unnarrowedById.put(metric.get("metric").asString(), metric));
        assertThat(unnarrowedById).containsKey(CPU).doesNotContainKey("monitor_container_cpu_usage");
        assertThat(unnarrowedById.get("monitor_smart_data_written_bytes").has("series")).isFalse();

        McpTestSupport.ToolResult result = ok(this.all, "list_metrics", args("serverId", this.server.serverId(),
                "metrics", List.of(CPU, "monitor_smart_data_written_bytes"), "includeSeries", true));
        JsonNode metrics = this.objectMapper.readTree(result.text());

        Map<String, JsonNode> byId = new HashMap<>();
        metrics.forEach(metric -> byId.put(metric.get("metric").asString(), metric));
        assertThat(byId).containsOnlyKeys(CPU, "monitor_smart_data_written_bytes");
        List<String> devices = new ArrayList<>();
        byId.get("monitor_smart_data_written_bytes").get("series")
                .forEach(labels -> devices.add(labels.get("device").asString()));
        assertThat(devices).containsExactlyInAnyOrder("sda", "sdf", "nvme0");

        assertThat(this.mcp.call(this.other, "list_metrics", args("serverId", this.server.serverId())).isError())
                .isTrue();
    }

    @Test
    void queryMetricsReturnsSeriesWithStatistics() throws Exception {
        backfill(false);

        McpTestSupport.ToolResult result = awaitResult("query_metrics",
                args("serverId", this.server.serverId(), "metric", CPU), r -> r.text().contains("\"last\""));
        JsonNode query = this.objectMapper.readTree(result.text());

        assertThat(query.get("metric").asString()).isEqualTo(CPU);
        assertThat(query.get("series").size()).isEqualTo(1);
        JsonNode series = query.get("series").get(0);
        assertThat(series.get("last").asDouble()).isEqualTo(12.5);
        assertThat(series.get("min").asDouble()).isEqualTo(12.5);
        assertThat(series.get("maxAt").asLong()).isBetween(query.get("from").asLong(), query.get("to").asLong());
        assertThat(series.get("minAt").asLong()).isBetween(query.get("from").asLong(), query.get("to").asLong());
        assertThat(series.has("points")).isFalse();
        assertThat(series.get("labels").has("__name__")).isFalse();
        assertThat(query.get("stepSeconds").asInt()).isGreaterThanOrEqualTo(15);
        assertThat(query.get("truncated").asBoolean()).isFalse();
    }

    @Test
    void queryMetricsReturnsPointsAsWholeSecondsWhenAsked() throws Exception {
        backfill(false);

        McpTestSupport.ToolResult result = awaitResult("query_metrics",
                args("serverId", this.server.serverId(), "metric", CPU, "includePoints", true),
                r -> r.text().contains("\"points\""));
        JsonNode point = this.objectMapper.readTree(result.text()).get("series").get(0).get("points").get(0);

        assertThat(point.size()).isEqualTo(2);
        assertThat(point.get(0).isIntegralNumber()).isTrue();
    }

    @Test
    void queryMetricsLimitsPointsOverALongRange() throws Exception {
        ingest(TestFixtures.minimalIngestPayload());

        McpTestSupport.ToolResult result = ok(this.all, "query_metrics", args(
                "serverId", this.server.serverId(), "metric", CPU,
                "from", java.time.Instant.now().minus(java.time.Duration.ofDays(30)).toString(), "stepSeconds", 15, "includePoints", true));
        JsonNode query = this.objectMapper.readTree(result.text());

        assertThat(query.get("stepSeconds").asInt()).isGreaterThanOrEqualTo(30 * 86_400 / 200);
        query.get("series").forEach(series -> assertThat(series.get("points").size()).isLessThanOrEqualTo(201));
    }

    @Test
    void queryMetricsValidatesItsInput() throws Exception {
        long id = this.server.serverId();

        McpTestSupport.ToolResult unknownMetric = this.mcp.call(this.all, "query_metrics", args("serverId", id, "metric", "nope"));
        assertThat(unknownMetric.isError()).isTrue();
        assertThat(unknownMetric.text()).contains("list_metrics");

        McpTestSupport.ToolResult badLabel = this.mcp.call(this.all, "query_metrics",
                args("serverId", id, "metric", "monitor_disk_usage_percent", "labels", Map.of("nope", "x")));
        assertThat(badLabel.isError()).isTrue();
        assertThat(badLabel.text()).contains("disk");

        McpTestSupport.ToolResult labelOnUnlabelled = this.mcp.call(this.all, "query_metrics",
                args("serverId", id, "metric", CPU, "labels", Map.of("disk", "/")));
        assertThat(labelOnUnlabelled.isError()).isTrue();
        assertThat(labelOnUnlabelled.text()).contains("no labels");

        assertThat(this.mcp.call(this.all, "query_metrics", args("serverId", id, "metric", CPU,
                "from", "2026-02-01T00:00:00Z", "to", "2026-01-01T00:00:00Z")).text()).contains("before");
        assertThat(this.mcp.call(this.all, "query_metrics", args("serverId", id, "metric", CPU,
                "from", "yesterday")).text()).contains("ISO-8601");
        assertThat(this.mcp.call(this.all, "query_metrics", args("serverId", id, "metric", CPU,
                "from", "2020-01-01T00:00:00Z")).text()).contains("at most");
    }

    @Test
    void queryMetricsDeniesOtherUsers() throws Exception {
        McpTestSupport.ToolResult denied = this.mcp.call(this.other, "query_metrics",
                args("serverId", this.server.serverId(), "metric", CPU));
        assertThat(denied.isError()).isTrue();
    }

    @Test
    void alertRuleLifecycle() throws Exception {
        JsonNode groups = json(ok(this.all, "list_alert_groups", Map.of()));
        long groupId = groups.get(0).get("id").asLong();
        assertThat(groups.get(0).get("name").asString()).isEqualTo("Default");

        JsonNode created = json(ok(this.all, "create_alert_rule", args(
                "groupId", groupId, "metric", CPU, "operator", "MORE_THAN", "threshold", 95,
                "severity", "CRITICAL", "name", "CPU very high", "windowSeconds", 60)));
        long ruleId = created.get("id").asLong();
        assertThat(created.get("name").asString()).isEqualTo("CPU very high");
        assertThat(created.get("enabled").asBoolean()).isTrue();

        JsonNode rules = json(ok(this.all, "list_alert_rules", args("ruleId", ruleId)));
        assertThat(rules.size()).isEqualTo(1);
        assertThat(rules.get(0).get("threshold").asDouble()).isEqualTo(95);

        JsonNode updated = json(ok(this.all, "update_alert_rule", args("ruleId", ruleId, "threshold", 80, "enabled", false)));
        assertThat(updated.get("threshold").asDouble()).isEqualTo(80);
        assertThat(updated.get("enabled").asBoolean()).isFalse();
        assertThat(updated.get("name").asString()).as("untouched fields keep their value").isEqualTo("CPU very high");
        assertThat(updated.get("severity").asString()).isEqualTo("CRITICAL");
        assertThat(updated.get("windowSeconds").asInt()).isEqualTo(60);

        assertThat(json(ok(this.all, "list_alert_rules", args("groupId", groupId))).toString()).contains("CPU very high");

        ok(this.all, "delete_alert_rule", args("ruleId", ruleId));
        assertThat(json(ok(this.all, "list_alert_rules", args("groupId", groupId))).toString())
                .doesNotContain("CPU very high");
        assertThat(this.mcp.call(this.all, "list_alert_rules", args("ruleId", ruleId)).isError()).isTrue();
    }

    @Test
    void createAlertRuleUsesTheSameValidationAsTheRestApi() throws Exception {
        long groupId = json(ok(this.all, "list_alert_groups", Map.of())).get(0).get("id").asLong();

        McpTestSupport.ToolResult result = this.mcp.call(this.all, "create_alert_rule", args(
                "groupId", groupId, "metric", "monitor_does_not_exist", "operator", "MORE_THAN", "threshold", 1,
                "severity", "WARNING"));

        assertThat(result.isError()).isTrue();
        assertThat(result.text()).containsIgnoringCase("metric");
    }

    @Test
    void alertToolsCannotTouchAnotherUsersAlerts() throws Exception {
        long groupId = json(ok(this.all, "list_alert_groups", Map.of())).get(0).get("id").asLong();
        long ruleId = json(ok(this.all, "create_alert_rule", args("groupId", groupId, "metric", CPU,
                "operator", "MORE_THAN", "threshold", 95, "severity", "WARNING"))).get("id").asLong();

        assertThat(json(ok(this.other, "list_alert_groups", Map.of())).toString()).doesNotContain("alpha");
        assertThat(this.mcp.call(this.other, "list_alert_rules", args("ruleId", ruleId)).isError()).isTrue();
        assertThat(this.mcp.call(this.other, "update_alert_rule", args("ruleId", ruleId, "threshold", 1)).isError()).isTrue();
        assertThat(this.mcp.call(this.other, "delete_alert_rule", args("ruleId", ruleId)).isError()).isTrue();
        assertThat(this.mcp.call(this.other, "set_alert_group_servers", args("groupId", groupId, "remove", List.of(1))).isError())
                .isTrue();
        assertThat(json(ok(this.all, "list_alert_rules", args("ruleId", ruleId))).size()).isEqualTo(1);
    }

    @Test
    void setAlertGroupServersAddsAndRemovesMembers() throws Exception {
        JsonNode group = json(ok(this.all, "list_alert_groups", Map.of())).get(0);
        long groupId = group.get("id").asLong();
        long serverId = this.server.serverId();
        assertThat(group.get("servers").toString()).as("auto-add puts the new server in the default group")
                .contains("\"id\":" + serverId);

        JsonNode removed = json(ok(this.all, "set_alert_group_servers", args("groupId", groupId, "remove", List.of(serverId))));
        assertThat(removed.get("servers").size()).isZero();

        JsonNode added = json(ok(this.all, "set_alert_group_servers", args("groupId", groupId, "add", List.of(serverId))));
        assertThat(added.get("servers").size()).isEqualTo(1);

        assertThat(this.mcp.call(this.all, "set_alert_group_servers", args("groupId", groupId)).isError()).isTrue();
        assertThat(this.mcp.call(this.all, "set_alert_group_servers",
                args("groupId", groupId, "add", List.of(serverId), "remove", List.of(serverId))).isError()).isTrue();
        assertThat(this.mcp.call(this.all, "set_alert_group_servers",
                args("groupId", groupId, "add", List.of(99_999))).isError()).isTrue();
    }

    @Test
    void previewAlertRuleSummarisesHistoryWithoutRawSamples() throws Exception {
        backfill(false);
        awaitResult("query_metrics", args("serverId", this.server.serverId(), "metric", CPU),
                r -> r.text().contains("\"last\""));

        JsonNode preview = json(ok(this.all, "preview_alert_rule", args(
                "serverId", this.server.serverId(), "metric", CPU, "operator", "MORE_THAN", "threshold", 5,
                "rangeHours", 1)));

        assertThat(preview.get("timesFired").asInt()).isGreaterThan(0);
        JsonNode series = preview.get("series").get(0);
        assertThat(series.get("max").asDouble()).isEqualTo(12.5);
        assertThat(series.has("values")).isFalse();
        assertThat(series.get("firing").size()).isGreaterThan(0);

        JsonNode quiet = json(ok(this.all, "preview_alert_rule", args(
                "serverId", this.server.serverId(), "metric", CPU, "operator", "MORE_THAN", "threshold", 90)));
        assertThat(quiet.get("timesFired").asInt()).isZero();
    }

    @Test
    void listAlertEventsAndDestinationsWorkWhenEmpty() throws Exception {
        JsonNode events = json(ok(this.all, "list_alert_events", args("state", "firing")));
        assertThat(events.get("totalItems").asLong()).isZero();
        assertThat(json(ok(this.all, "list_alert_destinations", Map.of())).size()).isZero();
        assertThat(this.mcp.call(this.all, "list_alert_events", args("state", "bogus")).isError()).isTrue();
    }

    @Test
    void unsilencingAnUnknownEventReturnsAToolError() throws Exception {
        McpTestSupport.ToolResult result = this.mcp.call(this.all, "set_alert_event_silenced",
                args("eventId", 12345, "silenced", false));
        assertThat(result.isError()).isTrue();
        assertThat(result.text()).isNotBlank();
    }

    @Test
    void testingAnUnknownDestinationReturnsAToolError() throws Exception {
        assertThat(this.mcp.call(this.all, "test_alert_destination", args("destinationId", 12345)).isError()).isTrue();
    }
}
