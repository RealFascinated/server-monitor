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
 * Guards the rule that every tool checks its scope first, and that {@code tools/list} agrees with it. For each scope,
 * a token holding only that scope must see exactly the tools it can call, and every other tool must refuse it even
 * though it is hidden. A new tool that forgets {@code McpAuth.require}, or whose {@code @RequiresScope} disagrees with
 * it, fails here.
 */
class McpToolScopeCoverageTest extends IntegrationTestBase {
    private static final Map<String, Set<String>> VISIBLE_TOOLS = Map.of(
            "servers:read", Set.of("list_servers", "list_server_incidents"),
            "metrics:read", Set.of("list_metrics", "query_metrics"),
            "alerts:read", Set.of("list_alert_events", "list_alert_groups", "list_alert_rules",
                    "list_alert_destinations", "preview_alert_rule"),
            "alerts:write", Set.of("list_alert_events", "list_alert_groups", "list_alert_rules",
                    "list_alert_destinations", "preview_alert_rule", "set_alert_event_silenced",
                    "create_alert_rule", "update_alert_rule", "delete_alert_rule", "set_alert_group_servers",
                    "test_alert_destination"));

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listedToolsAreExactlyTheOnesATokenCanCall() throws Exception {
        TestAuthSupport auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        McpTestSupport mcp = new McpTestSupport(this.mockMvc, this.objectMapper);
        String session = auth.register(TestAuthSupport.uniqueEmail(), "password-1234").token();

        String everything = mcp.createToken(session, "servers:read", "metrics:read", "alerts:write");
        Map<String, JsonNode> allTools = listTools(mcp, everything);
        assertThat(allTools).hasSize(15);

        for (Map.Entry<String, Set<String>> entry : VISIBLE_TOOLS.entrySet()) {
            String token = mcp.createToken(session, entry.getKey());
            assertThat(listTools(mcp, token).keySet()).as("tools listed for %s", entry.getKey())
                    .isEqualTo(entry.getValue());
            for (Map.Entry<String, JsonNode> tool : allTools.entrySet()) {
                if (entry.getValue().contains(tool.getKey())) {
                    continue;
                }
                McpTestSupport.ToolResult result = mcp.call(token, tool.getKey(),
                        dummyArguments(tool.getValue().get("inputSchema")));
                assertThat(result.isError()).as("%s must reject a %s token", tool.getKey(), entry.getKey()).isTrue();
                assertThat(result.text()).as("%s error", tool.getKey()).contains("missing the");
            }
        }
    }

    @Test
    void everyToolIsListedForSomeScope() throws Exception {
        Set<String> covered = new HashSet<>();
        VISIBLE_TOOLS.values().forEach(covered::addAll);
        assertThat(covered).hasSize(15);
    }

    private Map<String, JsonNode> listTools(McpTestSupport mcp, String token) throws Exception {
        Map<String, JsonNode> tools = new HashMap<>();
        for (JsonNode tool : mcp.body(mcp.rpc(token, "tools/list", Map.of())).get("result").get("tools")) {
            tools.put(tool.get("name").asString(), tool);
        }
        return tools;
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
