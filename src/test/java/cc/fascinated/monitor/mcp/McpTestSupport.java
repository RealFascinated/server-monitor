package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.support.TestAuthSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Talks to the real {@code /mcp} endpoint the way an MCP client does: JSON-RPC {@code tools/call} over HTTP. */
public final class McpTestSupport {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public McpTestSupport(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /** Creates an API token through the REST API (session auth) and returns the raw token. */
    public String createToken(String sessionToken, String... scopes) throws Exception {
        MvcResult result = this.mockMvc.perform(post("/v1/user/api-tokens")
                        .header("Authorization", TestAuthSupport.bearer(sessionToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(Map.of("name", "test", "scopes", List.of(scopes)))))
                .andExpect(status().isOk())
                .andReturn();
        return this.objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    public MvcResult rpc(String apiToken, String method, Map<String, Object> params) throws Exception {
        var request = post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(this.objectMapper.writeValueAsString(Map.of(
                        "jsonrpc", "2.0", "id", 1, "method", method, "params", params)));
        if (apiToken != null) {
            request.header("Authorization", TestAuthSupport.bearer(apiToken));
        }
        return this.mockMvc.perform(request).andReturn();
    }

    /** The JSON-RPC response body of a call, which may arrive as plain JSON or as one SSE event. */
    public JsonNode body(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        for (String line : content.split("\n")) {
            if (line.startsWith("data:")) {
                content = line.substring("data:".length()).trim();
                break;
            }
        }
        return this.objectMapper.readTree(content);
    }

    public record ToolResult(boolean isError, String text, JsonNode raw) {
        public JsonNode json(ObjectMapper mapper) {
            return mapper.readTree(this.text);
        }
    }

    public ToolResult call(String apiToken, String tool, Map<String, Object> arguments) throws Exception {
        MvcResult result = rpc(apiToken, "tools/call", Map.of("name", tool, "arguments", arguments));
        JsonNode body = body(result);
        if (body.has("error")) {
            return new ToolResult(true, body.get("error").toString(), body);
        }
        JsonNode payload = body.get("result");
        boolean isError = payload.has("isError") && payload.get("isError").asBoolean();
        JsonNode content = payload.get("content");
        String text = content != null && content.size() > 0 ? content.get(0).get("text").asString() : "";
        return new ToolResult(isError, text, payload);
    }
}
