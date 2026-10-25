package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpAuthIntegrationTest extends IntegrationTestBase {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestAuthSupport auth;
    private McpTestSupport mcp;
    private String session;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.mcp = new McpTestSupport(this.mockMvc, this.objectMapper);
        this.session = this.auth.register(TestAuthSupport.uniqueEmail(), "password-1234").token();
    }

    private int statusOf(String bearer) throws Exception {
        return this.mcp.rpc(bearer, "tools/list", Map.of()).getResponse().getStatus();
    }

    @Test
    void rejectsRequestsWithoutAValidApiToken() throws Exception {
        assertThat(statusOf(null)).isEqualTo(401);
        assertThat(statusOf("smk_not-a-real-token")).isEqualTo(401);
        assertThat(statusOf("not-even-prefixed")).isEqualTo(401);
        assertThat(this.mcp.rpc(null, "tools/list", Map.of()).getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo("Bearer");
    }

    @Test
    void aSessionTokenDoesNotWorkOnMcp() throws Exception {
        assertThat(statusOf(this.session)).isEqualTo(401);
    }

    @Test
    void revokedAndExpiredTokensAreRejected() throws Exception {
        String revoked = this.mcp.createToken(this.session, "servers:read");
        String expired = this.mcp.createToken(this.session, "servers:read");
        String live = this.mcp.createToken(this.session, "servers:read");
        assertThat(statusOf(revoked)).isEqualTo(200);

        this.jdbcTemplate.update("UPDATE api_tokens SET revoked_at = NOW() WHERE id = 1");
        this.jdbcTemplate.update("UPDATE api_tokens SET expires_at = NOW() - INTERVAL '1 minute' WHERE id = 2");

        assertThat(statusOf(revoked)).isEqualTo(401);
        assertThat(statusOf(expired)).isEqualTo(401);
        assertThat(statusOf(live)).isEqualTo(200);
    }

    @Test
    void recordsWhenATokenWasLastUsed() throws Exception {
        String token = this.mcp.createToken(this.session, "servers:read");
        assertThat(this.jdbcTemplate.queryForObject("SELECT last_used_at FROM api_tokens", java.sql.Timestamp.class))
                .isNull();

        assertThat(statusOf(token)).isEqualTo(200);

        assertThat(this.jdbcTemplate.queryForObject("SELECT last_used_at FROM api_tokens", java.sql.Timestamp.class))
                .isNotNull();
    }

    @Test
    void aToolWithoutItsScopeFailsNamingTheScope() throws Exception {
        String token = this.mcp.createToken(this.session, "servers:read");

        McpTestSupport.ToolResult result = this.mcp.call(token, "list_alert_groups", Map.of());

        assertThat(result.isError()).isTrue();
        assertThat(result.text()).contains("alerts:read");
    }

    @Test
    void alertsWriteImpliesAlertsRead() throws Exception {
        String token = this.mcp.createToken(this.session, "alerts:write");

        McpTestSupport.ToolResult result = this.mcp.call(token, "list_alert_groups", Map.of());

        assertThat(result.isError()).isFalse();
        assertThat(result.text()).contains("Default");
    }

    @Test
    void alertsReadDoesNotImplyAlertsWrite() throws Exception {
        String token = this.mcp.createToken(this.session, "alerts:read");

        McpTestSupport.ToolResult result = this.mcp.call(token, "delete_alert_rule", Map.of("ruleId", 1));

        assertThat(result.isError()).isTrue();
        assertThat(result.text()).contains("alerts:write");
    }
}
