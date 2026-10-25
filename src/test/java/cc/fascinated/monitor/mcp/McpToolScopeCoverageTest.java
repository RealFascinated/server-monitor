package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the rule that every tool checks its scope first. A token holding only servers:read is used to call every
 * tool the server lists; any tool that is not a servers:read tool must refuse it, so a new tool that forgets
 * {@code McpAuth.require} fails here.
 */
class McpToolScopeCoverageTest extends IntegrationTestBase {
    private static final Set<String> SERVERS_READ_TOOLS = Set.of("list_servers", "list_server_incidents");

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void everyToolOutsideServersReadRefusesAServersReadToken() throws Exception {
        TestAuthSupport auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        McpTestSupport mcp = new McpTestSupport(this.mockMvc, this.objectMapper);
        String session = auth.register(TestAuthSupport.uniqueEmail(), "password-1234").token();
        String token = mcp.createToken(session, "servers:read");

        JsonNode tools = mcp.body(mcp.rpc(token, "tools/list", Map.of())).get("result").get("tools");
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            String name = tool.get("name").asString();
            names.add(name);
            if (SERVERS_READ_TOOLS.contains(name)) {
                continue;
            }
            McpTestSupport.ToolResult result = mcp.call(token, name, dummyArguments(tool.get("inputSchema")));
            assertThat(result.isError()).as("%s must reject a token without its scope", name).isTrue();
            assertThat(result.text()).as("%s error", name).contains("missing the");
        }
        assertThat(names).containsAll(SERVERS_READ_TOOLS).hasSize(15);
    }

    /** Type-correct placeholder values for every required argument. */
    private static Map<String, Object> dummyArguments(JsonNode schema) {
        Map<String, Object> arguments = new HashMap<>();
        JsonNode required = schema.get("required");
        if (required == null) {
            return arguments;
        }
        for (JsonNode field : required) {
            String name = field.asString();
            JsonNode property = schema.get("properties").get(name);
            if (property.has("enum")) {
                arguments.put(name, property.get("enum").get(0).asString());
                continue;
            }
            arguments.put(name, switch (property.get("type").asString()) {
                case "integer", "number" -> 1;
                case "boolean" -> true;
                case "array" -> java.util.List.of();
                case "object" -> Map.of();
                default -> "x";
            });
        }
        return arguments;
    }
}
