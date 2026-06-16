package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerControllerIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password123";

    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;
    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.bearerToken = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();
    }

    @Test
    void createGetRenameAndDeleteServer() throws Exception {
        var created = this.auth.createServer(this.bearerToken, "prod-1");

        this.mockMvc.perform(get("/v1/servers/{serverId}", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverId").value(created.serverId()))
                .andExpect(jsonPath("$.serverName").value("prod-1"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        this.mockMvc.perform(post("/v1/servers/{serverId}/rename", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"prod-renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverName").value("prod-renamed"));

        this.mockMvc.perform(delete("/v1/servers/{serverId}", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/v1/servers/{serverId}", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        this.mockMvc.perform(get("/v1/servers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void heartbeatAcceptsIngestToken() throws Exception {
        var created = this.auth.createServer(this.bearerToken, "agent-host");

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(created.ingestToken().toString())))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/servers/{serverId}/status", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void listsIncidents() throws Exception {
        var created = this.auth.createServer(this.bearerToken, "incident-host");

        this.mockMvc.perform(get("/v1/servers/{serverId}/incidents", created.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .param("page", "1")
                        .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
