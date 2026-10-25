package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpEndpointIntegrationTest extends IntegrationTestBase {
    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;
    private McpTestSupport mcp;

    @BeforeEach
    void setUp() {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.mcp = new McpTestSupport(this.mockMvc, this.objectMapper);
    }

    @Test
    void listsToolsWithDescriptions() throws Exception {
        String session = this.auth.register(TestAuthSupport.uniqueEmail(), "password-1234").token();
        String token = this.mcp.createToken(session, "servers:read");
        JsonNode body = this.mcp.body(this.mcp.rpc(token, "tools/list", Map.of()));
        assertThat(body.get("result").get("tools").toString()).contains("list_servers");
    }

    @Test
    void callsAToolAsTheTokensUser() throws Exception {
        String session = this.auth.register(TestAuthSupport.uniqueEmail(), "password-1234").token();
        this.auth.createServer(session, "alpha");
        String token = this.mcp.createToken(session, "servers:read");

        McpTestSupport.ToolResult result = this.mcp.call(token, "list_servers", Map.of());

        assertThat(result.isError()).isFalse();
        assertThat(result.text()).contains("alpha");
    }
}
