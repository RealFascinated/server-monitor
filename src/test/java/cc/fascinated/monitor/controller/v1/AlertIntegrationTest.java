package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.alert.AlertStateSweeper;
import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password123";
    private static final String CPU = "monitor_host_cpu_usage";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AlertStateSweeper sweeper;
    @Autowired
    private cc.fascinated.monitor.alert.DefaultAlertRuleService defaultAlertRuleService;

    private TestAuthSupport auth;
    private String token;
    private HttpServer webhook;
    private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.token = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();
        this.webhook = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.webhook.createContext("/api/webhooks", exchange -> {
            this.received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        this.webhook.start();
    }

    @AfterEach
    void tearDown() {
        this.webhook.stop(0);
    }

    private String webhookUrl() {
        return "http://127.0.0.1:%d/api/webhooks/123/secret-token".formatted(this.webhook.getAddress().getPort());
    }

    private long createDestination() throws Exception {
        String body = this.mockMvc.perform(post("/v1/alerts/destinations")
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ops","type":"DISCORD_WEBHOOK","config":"%s"}
                                """.formatted(webhookUrl())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return this.objectMapper.readTree(body).get("id").asLong();
    }

    private long createGroup(String bearer, String name, boolean autoAdd, Long... serverIds) throws Exception {
        String ids = java.util.Arrays.stream(serverIds).map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String body = this.mockMvc.perform(post("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(bearer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","autoAddServers":%s,"serverIds":[%s]}
                                """.formatted(name, autoAdd, ids)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return this.objectMapper.readTree(body).get("id").asLong();
    }

    /** Empties the seeded Default group and drops the rest, so a test controls exactly which rules exist. */
    private void clearDefaultGroup(String bearer) throws Exception {
        String body = this.mockMvc.perform(get("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(bearer)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode group : this.objectMapper.readTree(body)) {
            long id = group.get("id").asLong();
            if (group.get("defaultGroup").asBoolean()) {
                String rules = this.mockMvc.perform(get("/v1/alerts/rules").param("groupId", String.valueOf(id))
                                .header("Authorization", TestAuthSupport.bearer(bearer)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                for (JsonNode rule : this.objectMapper.readTree(rules)) {
                    this.mockMvc.perform(delete("/v1/alerts/rules/{id}", rule.get("id").asLong())
                                    .header("Authorization", TestAuthSupport.bearer(bearer)))
                            .andExpect(status().isOk());
                }
                continue;
            }
            this.mockMvc.perform(delete("/v1/alerts/groups/{id}", id)
                            .header("Authorization", TestAuthSupport.bearer(bearer)))
                    .andExpect(status().isOk());
        }
    }

    private void setGroupEnabled(long groupId, boolean enabled) throws Exception {
        this.mockMvc.perform(patch("/v1/alerts/groups/{id}", groupId)
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":%s}".formatted(enabled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(enabled));
    }

    private long createRule(String bearer, long groupId, Long destinationId, String metric, String filter,
                            String operator, double threshold, boolean enabled) throws Exception {
        String body = this.mockMvc.perform(post("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(bearer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":%d,"destinationId":%s,"name":"rule","metric":"%s","labelFilter":%s,
                                 "operator":"%s","threshold":%s,"windowSeconds":0,"severity":"WARNING","enabled":%s}
                                """.formatted(groupId, destinationId, metric, filter, operator, threshold, enabled)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return this.objectMapper.readTree(body).get("id").asLong();
    }

    private void ingestCpu(String ingestToken, double cpu) throws Exception {
        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(ingestToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.minimalIngestPayload().replace("\"cpuUsage\": 12.5", "\"cpuUsage\": " + cpu)))
                .andExpect(status().isOk());
    }

    private String nextMessage() throws Exception {
        return this.received.poll(5, TimeUnit.SECONDS);
    }

    private int eventCount(String where) {
        Integer count = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alert_events WHERE " + where, Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    void newUsersGetADefaultGroupWithDefaultRulesAndNoDestination() throws Exception {
        String groups = this.mockMvc.perform(get("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Default"))
                .andExpect(jsonPath("$[0].autoAddServers").value(true))
                .andExpect(jsonPath("$[0].defaultGroup").value(true))
                .andExpect(jsonPath("$[0].enabled").value(false))
                .andReturn().getResponse().getContentAsString();
        long groupId = this.objectMapper.readTree(groups).get(0).get("id").asLong();

        String body = this.mockMvc.perform(get("/v1/alerts/rules").param("groupId", String.valueOf(groupId))
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.Set<String> metrics = new java.util.HashSet<>();
        for (JsonNode rule : this.objectMapper.readTree(body)) {
            metrics.add(rule.get("metric").asString());
            assertThat(rule.get("destinationId") == null || rule.get("destinationId").isNull()).isTrue();
            assertThat(rule.get("groupId").asLong()).isEqualTo(groupId);
        }
        assertThat(metrics).contains(CPU, "monitor_host_mem_usage_pct", "monitor_disk_usage_percent",
                "monitor_host_temperature_celsius", "monitor_server_up", "monitor_smart_health_passed",
                "monitor_host_oom_kills_per_second", "monitor_smart_temperature_celsius",
                "monitor_smart_reallocated_sectors", "monitor_smart_pending_sectors",
                "monitor_smart_reported_uncorrect", "monitor_smart_offline_uncorrectable",
                "monitor_smart_udma_crc_errors", "monitor_smart_endurance_used_percent",
                "monitor_smart_percentage_used", "monitor_smart_available_spare", "monitor_smart_media_errors");
        // NVMe logs benign entries all the time, so this one stays opt-in
        assertThat(metrics).doesNotContain("monitor_smart_error_log_entries");
    }

    @Test
    void newServersJoinAutoAddGroupsOnly() throws Exception {
        long manual = createGroup(this.token, "manual", false);
        var server = this.auth.createServer(this.token, "fresh");

        String groups = this.mockMvc.perform(get("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode group : this.objectMapper.readTree(groups)) {
            boolean contains = false;
            for (JsonNode member : group.get("servers")) {
                contains |= member.get("id").asLong() == server.serverId();
            }
            assertThat(contains).as(group.get("name").asString()).isEqualTo(group.get("id").asLong() != manual);
        }
    }

    @Test
    void firstDestinationIsAttachedToRulesWithoutOne() throws Exception {
        long destinationId = createDestination();

        String body = this.mockMvc.perform(get("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode rule : this.objectMapper.readTree(body)) {
            assertThat(rule.get("destinationId").asLong()).isEqualTo(destinationId);
        }
    }

    @Test
    void rulesMayBeCreatedWithoutADestination() throws Exception {
        long group = createGroup(this.token, "g", false);
        this.mockMvc.perform(post("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":%d,"metric":"%s","operator":"MORE_THAN","threshold":95,"severity":"WARNING"}
                                """.formatted(group, CPU)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationId").doesNotExist())
                .andExpect(jsonPath("$.groupName").value("g"));
    }

    @Test
    void destinationRejectsForeignHostsAndNeverEchoesTheUrl() throws Exception {
        this.mockMvc.perform(post("/v1/alerts/destinations")
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"bad","type":"DISCORD_WEBHOOK","config":"http://169.254.169.254/api/webhooks/1/x"}
                                """))
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(post("/v1/alerts/destinations")
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ops","type":"DISCORD_WEBHOOK","config":"%s"}
                                """.formatted(webhookUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedUrl").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-token"))));

        String list = this.mockMvc.perform(get("/v1/alerts/destinations")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain("secret-token");
    }

    @Test
    void groupRuleFiresOnceThenResolves() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "web-01");
        long destination = createDestination();
        long group = createGroup(this.token, "prod", false, server.serverId());
        long rule = createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        ingestCpu(server.ingestToken().toString(), 50);
        assertThat(nextMessage()).isNull();

        ingestCpu(server.ingestToken().toString(), 96.55);
        ingestCpu(server.ingestToken().toString(), 97.5);
        String firing = nextMessage();
        assertThat(firing).contains("web-01").contains("above").contains("96.55");
        assertThat(firing).contains("\"allowed_mentions\"");
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).isEqualTo(1);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("no second notification while still firing").isNull();

        ingestCpu(server.ingestToken().toString(), 40);
        String resolved = nextMessage();
        assertThat(resolved).contains("back below");
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NOT NULL AND resolve_reason = 'RECOVERED'")).isEqualTo(1);

        this.mockMvc.perform(get("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firingCount").value(0))
                .andExpect(jsonPath("$[0].lastFiredAt").exists());
    }

    @Test
    void accountsWithoutADefaultGroupGetOneWithTheirServers() throws Exception {
        var server = this.auth.createServer(this.token, "legacy");
        // an account that predates alerting has no groups at all
        this.jdbcTemplate.update("DELETE FROM alert_groups WHERE user_id = (SELECT user_id FROM server_members WHERE server_id = ? LIMIT 1)",
                server.serverId());

        this.defaultAlertRuleService.backfillMissing();
        this.defaultAlertRuleService.backfillMissing();

        this.mockMvc.perform(get("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].defaultGroup").value(true))
                .andExpect(jsonPath("$[0].enabled").value(false))
                .andExpect(jsonPath("$[0].servers[0].id").value(server.serverId()))
                .andExpect(jsonPath("$[0].ruleCount").value(org.hamcrest.Matchers.greaterThan(10)));
    }

    @Test
    void theDefaultGroupCannotBeDeleted() throws Exception {
        String groups = this.mockMvc.perform(get("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andReturn().getResponse().getContentAsString();
        long defaultId = this.objectMapper.readTree(groups).get(0).get("id").asLong();

        this.mockMvc.perform(delete("/v1/alerts/groups/{id}", defaultId)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isConflict());
        this.mockMvc.perform(get("/v1/alerts/groups/{id}", defaultId)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk());
    }

    @Test
    void disablingAGroupStopsItsRulesAndClosesOpenEvents() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "toggled");
        long destination = createDestination();
        long group = createGroup(this.token, "switchable", false, server.serverId());
        long rule = createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        ingestCpu(server.ingestToken().toString(), 99);
        assertThat(nextMessage()).contains("toggled");
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).isEqualTo(1);

        setGroupEnabled(group, false);
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).isZero();
        ingestCpu(server.ingestToken().toString(), 99);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("a disabled group never fires").isNull();
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).isZero();

        setGroupEnabled(group, true);
        ingestCpu(server.ingestToken().toString(), 99);
        assertThat(nextMessage()).contains("toggled");
    }

    @Test
    void everyServerInTheGroupInheritsItsRulesAndOthersDoNot() throws Exception {
        clearDefaultGroup(this.token);
        var first = this.auth.createServer(this.token, "alpha");
        var second = this.auth.createServer(this.token, "beta");
        var outside = this.auth.createServer(this.token, "gamma");
        long destination = createDestination();
        long group = createGroup(this.token, "pair", false, first.serverId(), second.serverId());
        createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        ingestCpu(outside.ingestToken().toString(), 99);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("gamma is not in the group").isNull();

        ingestCpu(first.ingestToken().toString(), 99);
        assertThat(nextMessage()).contains("alpha");
        ingestCpu(second.ingestToken().toString(), 99);
        assertThat(nextMessage()).contains("beta");
    }

    @Test
    void addingAndRemovingServersFromAGroupTakesEffectImmediately() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "roamer");
        long destination = createDestination();
        long group = createGroup(this.token, "g", false);
        long rule = createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        ingestCpu(server.ingestToken().toString(), 99);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("not a member yet").isNull();

        this.mockMvc.perform(patch("/v1/alerts/groups/{id}", group)
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serverIds\":[%d]}".formatted(server.serverId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].name").value("roamer"));
        ingestCpu(server.ingestToken().toString(), 99);
        assertThat(nextMessage()).contains("roamer");
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).isEqualTo(1);

        this.mockMvc.perform(patch("/v1/alerts/groups/{id}", group)
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serverIds\":[]}"))
                .andExpect(status().isOk());
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NULL")).as("open event closes").isZero();
    }

    @Test
    void deletingAGroupDeletesItsRules() throws Exception {
        var server = this.auth.createServer(this.token, "web");
        long group = createGroup(this.token, "temp", false, server.serverId());
        long rule = createRule(this.token, group, null, CPU, "{}", "MORE_THAN", 90, true);

        this.mockMvc.perform(delete("/v1/alerts/groups/{id}", group)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/v1/alerts/rules/{id}", rule)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void backfilledSamplesNeverFireAlerts() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "batch-host");
        long destination = createDestination();
        long group = createGroup(this.token, "g", false, server.serverId());
        createRule(this.token, group, destination, CPU, "{}", "LESS_THAN", 99, true);
        long now = Instant.now().getEpochSecond();

        this.mockMvc.perform(post("/v1/servers/ingest/batch")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.batchPayload(now, now - 300, now - 240)))
                .andExpect(status().isOk());

        assertThat(this.received.poll(1, TimeUnit.SECONDS)).isNull();
        assertThat(eventCount("1 = 1")).isZero();
    }

    @Test
    void labelFilterOnlyMatchesTheChosenSeries() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "disk-host");
        long destination = createDestination();
        long group = createGroup(this.token, "g", false, server.serverId());
        createRule(this.token, group, destination, "monitor_disk_usage_percent", "{\"disk\":\"/data\"}",
                "MORE_THAN", 80, true);

        String payload = TestFixtures.minimalIngestPayload().replace("\"diskMetrics\": []",
                "\"diskMetrics\": [{\"diskName\":\"/\",\"usedBytes\":95,\"totalBytes\":100},"
                        + "{\"diskName\":\"/data\",\"usedBytes\":50,\"totalBytes\":100}]");
        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("/ is over but is not the filtered disk").isNull();

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.replace("\"usedBytes\":50", "\"usedBytes\":85")))
                .andExpect(status().isOk());
        String message = nextMessage();
        assertThat(message).contains("disk-host").contains("/data");
    }

    @Test
    void leavingAServerRemovesItFromTheMembersGroups() throws Exception {
        var server = this.auth.createServer(this.token, "shared");
        String memberEmail = TestAuthSupport.uniqueEmail("member");
        String memberToken = this.auth.register(memberEmail, PASSWORD).token();
        String invite = this.mockMvc.perform(post("/v1/servers/{id}/members/invite", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(memberEmail)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode inviteJson = this.objectMapper.readTree(invite);
        this.mockMvc.perform(post("/v1/user/invites/{id}/accept", inviteJson.get("inviteId").asLong())
                        .header("Authorization", TestAuthSupport.bearer(memberToken)))
                .andExpect(status().isOk());

        // the accepted invite joined the member's auto-add Default group
        assertThat(this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM alert_group_servers gs JOIN alert_groups g ON g.id = gs.group_id
                WHERE g.user_id = (SELECT id FROM users WHERE email = ?) AND gs.server_id = ?
                """, Integer.class, memberEmail, server.serverId())).isEqualTo(1);

        this.mockMvc.perform(delete("/v1/servers/{id}/leave", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(memberToken)))
                .andExpect(status().isNoContent());

        assertThat(this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM alert_group_servers gs JOIN alert_groups g ON g.id = gs.group_id
                WHERE g.user_id = (SELECT id FROM users WHERE email = ?) AND gs.server_id = ?
                """, Integer.class, memberEmail, server.serverId())).isZero();
        // the owner's group membership is unaffected
        assertThat(this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alert_group_servers WHERE server_id = ?", Integer.class, server.serverId())).isEqualTo(1);
    }

    @Test
    void groupsCannotAddServersTheUserCannotSee() throws Exception {
        var server = this.auth.createServer(this.token, "private");
        String other = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();

        this.mockMvc.perform(post("/v1/alerts/groups")
                        .header("Authorization", TestAuthSupport.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"sneaky\",\"serverIds\":[%d]}".formatted(server.serverId())))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void foreignUsersCannotTouchRulesGroupsOrDestinations() throws Exception {
        long destination = createDestination();
        long group = createGroup(this.token, "mine", false);
        long rule = createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);
        String other = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();

        this.mockMvc.perform(get("/v1/alerts/rules/{id}", rule)
                        .header("Authorization", TestAuthSupport.bearer(other)))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/v1/alerts/groups/{id}", group)
                        .header("Authorization", TestAuthSupport.bearer(other)))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(delete("/v1/alerts/destinations/{id}", destination)
                        .header("Authorization", TestAuthSupport.bearer(other)))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":%d,"destinationId":%d,"metric":"%s","operator":"MORE_THAN","threshold":1,"severity":"WARNING"}
                                """.formatted(group, destination, CPU)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deletingADestinationInUseNeedsForce() throws Exception {
        clearDefaultGroup(this.token);
        long destination = createDestination();
        long group = createGroup(this.token, "g", false);
        createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        this.mockMvc.perform(delete("/v1/alerts/destinations/{id}", destination)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isConflict());
        this.mockMvc.perform(delete("/v1/alerts/destinations/{id}", destination).param("force", "true")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidRulesAreRejected() throws Exception {
        long destination = createDestination();
        long group = createGroup(this.token, "g", false);
        String base = """
                {"groupId":%d,"destinationId":%d,"metric":"%s","labelFilter":%s,"operator":"MORE_THAN","threshold":%s,
                 "recoveryThreshold":%s,"windowSeconds":%d,"severity":"WARNING"}
                """;
        assertRejected(base.formatted(group, destination, "not_a_metric", "{}", 1, "null", 0));
        assertRejected(base.formatted(group, destination, CPU, "{\"nope\":\"x\"}", 1, "null", 0));
        assertRejected(base.formatted(group, destination, CPU, "{}", 90, "95", 0));
        assertRejected(base.formatted(group, destination, CPU, "{}", 90, "null", 5));
    }

    private void assertRejected(String body) throws Exception {
        this.mockMvc.perform(post("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void offlineSweepKeepsServerUpRulesEvaluating() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "flaky");
        long destination = createDestination();
        long group = createGroup(this.token, "g", false, server.serverId());
        createRule(this.token, group, destination, "monitor_server_up", "{}", "LESS_THAN", 1, true);
        ingestCpu(server.ingestToken().toString(), 10);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).isNull();

        this.jdbcTemplate.update("UPDATE servers SET status = 1");
        this.sweeper.sweep();

        assertThat(nextMessage()).contains("flaky");
    }

    @Test
    void rulePatchKeepsItsGroupImmutable() throws Exception {
        long destination = createDestination();
        long group = createGroup(this.token, "g", false);
        long otherGroup = createGroup(this.token, "other", false);
        long rule = createRule(this.token, group, destination, CPU, "{}", "MORE_THAN", 90, true);

        this.mockMvc.perform(patch("/v1/alerts/rules/{id}", rule)
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":%d,"destinationId":%d,"metric":"%s","operator":"MORE_THAN","threshold":80,"severity":"CRITICAL"}
                                """.formatted(otherGroup, destination, CPU)))
                .andExpect(status().isBadRequest());
        this.mockMvc.perform(patch("/v1/alerts/rules/{id}", rule)
                        .header("Authorization", TestAuthSupport.bearer(this.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":%d,"destinationId":%d,"metric":"%s","operator":"MORE_THAN","threshold":80,"severity":"CRITICAL"}
                                """.formatted(group, destination, CPU)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(80.0))
                .andExpect(jsonPath("$.severity").value("CRITICAL"));
    }

    private long openEventId(long rule) {
        Long id = this.jdbcTemplate.queryForObject(
                "SELECT id FROM alert_events WHERE rule_id = ? AND resolved_at IS NULL", Long.class, rule);
        assertThat(id).isNotNull();
        return id;
    }

    private void silence(String bearer, long eventId) throws Exception {
        this.mockMvc.perform(post("/v1/alerts/events/{id}/silence", eventId)
                .header("Authorization", TestAuthSupport.bearer(bearer)))
                .andExpect(status().isOk());
    }

    @Test
    void silencedAlertStaysQuietUntilItGetsWorse() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "web-01");
        long group = createGroup(this.token, "prod", false, server.serverId());
        long rule = createRule(this.token, group, createDestination(), CPU, "{}", "MORE_THAN", 90, true);
        String ingest = server.ingestToken().toString();

        ingestCpu(ingest, 95);
        assertThat(nextMessage()).contains("above");
        long event = openEventId(rule);

        this.mockMvc.perform(post("/v1/alerts/events/{id}/silence", event)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk());
        assertThat(eventCount("id = " + event + " AND silenced_at IS NOT NULL AND silenced_value = 95")).isEqualTo(1);
        this.mockMvc.perform(get("/v1/alerts/rules")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(jsonPath("$[0].firingCount").value(0))
                .andExpect(jsonPath("$[0].silencedCount").value(1));
        this.mockMvc.perform(get("/v1/alerts/firing-count")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));

        ingestCpu(ingest, 95);
        ingestCpu(ingest, 93);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("same or better value stays silent").isNull();

        ingestCpu(ingest, 99);
        assertThat(nextMessage()).contains("got worse").contains("99");
        assertThat(eventCount("id = " + event + " AND silenced_at IS NULL AND peak_value = 99")).isEqualTo(1);
        this.mockMvc.perform(get("/v1/alerts/firing-count")
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void silencedAlertResolvesQuietlyAndFiresNormallyAgain() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "web-01");
        long group = createGroup(this.token, "prod", false, server.serverId());
        long rule = createRule(this.token, group, createDestination(), CPU, "{}", "MORE_THAN", 90, true);
        String ingest = server.ingestToken().toString();

        ingestCpu(ingest, 95);
        assertThat(nextMessage()).isNotNull();
        silence(this.token, openEventId(rule));

        ingestCpu(ingest, 40);
        assertThat(this.received.poll(1, TimeUnit.SECONDS)).as("a muted alert ends quietly").isNull();
        assertThat(eventCount("rule_id = " + rule + " AND resolved_at IS NOT NULL AND resolve_reason = 'RECOVERED'")).isEqualTo(1);

        ingestCpu(ingest, 96);
        assertThat(nextMessage()).as("the next firing is announced").contains("above");
    }

    @Test
    void unsilencingRestoresNotifications() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "web-01");
        long group = createGroup(this.token, "prod", false, server.serverId());
        long rule = createRule(this.token, group, createDestination(), CPU, "{}", "MORE_THAN", 90, true);
        String ingest = server.ingestToken().toString();

        ingestCpu(ingest, 95);
        assertThat(nextMessage()).isNotNull();
        long event = openEventId(rule);
        silence(this.token, event);
        this.mockMvc.perform(delete("/v1/alerts/events/{id}/silence", event)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isOk());
        assertThat(eventCount("id = " + event + " AND silenced_at IS NULL AND silenced_value IS NULL")).isEqualTo(1);

        ingestCpu(ingest, 40);
        assertThat(nextMessage()).as("resolves loudly again").contains("back below");
    }

    @Test
    void onlyTheOwnerCanSilenceAndOnlyOpenAlerts() throws Exception {
        clearDefaultGroup(this.token);
        var server = this.auth.createServer(this.token, "web-01");
        long group = createGroup(this.token, "prod", false, server.serverId());
        long rule = createRule(this.token, group, createDestination(), CPU, "{}", "MORE_THAN", 90, true);
        String ingest = server.ingestToken().toString();
        String other = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();

        ingestCpu(ingest, 95);
        assertThat(nextMessage()).isNotNull();
        long event = openEventId(rule);
        this.mockMvc.perform(post("/v1/alerts/events/{id}/silence", event)
                        .header("Authorization", TestAuthSupport.bearer(other)))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post("/v1/alerts/events/{id}/silence", 999_999_999L)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isNotFound());

        ingestCpu(ingest, 40);
        assertThat(nextMessage()).isNotNull();
        this.mockMvc.perform(post("/v1/alerts/events/{id}/silence", event)
                        .header("Authorization", TestAuthSupport.bearer(this.token)))
                .andExpect(status().isConflict());
    }
}
