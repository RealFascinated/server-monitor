package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerIngestIntegrationTest extends IntegrationTestBase {
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
    void ingestUpdatesServerInventory() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "ingest-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.minimalIngestPayload()))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/servers/{serverId}", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.agentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.inventory.osName").value("Linux"))
                .andExpect(jsonPath("$.inventory.cpuModel").value("Test CPU"));
    }

    @Test
    void rejectsInvalidIngestToken() throws Exception {
        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer("00000000-0000-0000-0000-000000000001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.minimalIngestPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotatedTokenInvalidatesPreviousToken() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "rotate-host");
        String originalToken = server.ingestToken().toString();

        var rotated = this.mockMvc.perform(post("/v1/servers/{serverId}/ingest-token/rotate", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverId").value(server.serverId()))
                .andReturn();

        String newToken = this.objectMapper.readTree(rotated.getResponse().getContentAsString())
                .get("ingestToken")
                .asText();

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(newToken)))
                .andExpect(status().isOk());

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(originalToken)))
                .andExpect(status().isUnauthorized());
    }
}
