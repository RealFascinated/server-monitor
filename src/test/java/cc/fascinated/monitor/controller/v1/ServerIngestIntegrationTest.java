package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void ingestBatchBackfillsSamplesAndReportsSkips() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "batch-host");
        long now = java.time.Instant.now().getEpochSecond();
        String body = TestFixtures.batchPayload(now, now - 300, now - 240, now - 7 * 3600);

        this.mockMvc.perform(post("/v1/servers/ingest/batch")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(2))
                .andExpect(jsonPath("$.skipped.too_old").value(1));
    }

    @Test
    void ingestBatchRejectsBatchWithoutSentAt() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "batch-no-sent-at");
        long now = java.time.Instant.now().getEpochSecond();

        this.mockMvc.perform(post("/v1/servers/ingest/batch")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.batchPayload(null, now - 10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestBatchRequiresIngestToken() throws Exception {
        long now = java.time.Instant.now().getEpochSecond();

        this.mockMvc.perform(post("/v1/servers/ingest/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.batchPayload(now, now - 10)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void liveIngestRejectsOversizedLabelPayload() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "bad-labels");
        String body = TestFixtures.minimalIngestPayload().replace("\"diskMetrics\": []",
                "\"diskMetrics\": [], \"dockerContainers\": [{\"containerName\": \"a\\nb\"}]");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestAcceptsClockStatusAndContainerIoRates() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "clock-io-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.ingestPayloadWithClockAndContainerIo()))
                .andExpect(status().isOk());
    }

    @Test
    void ingestAcceptsSmartMetrics() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "smart-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.ingestPayloadWithSmart()))
                .andExpect(status().isOk());
    }

    @Test
    void ingestRejectsSmartDeviceWithControlCharactersInLabel() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "smart-bad-label");
        String body = TestFixtures.ingestPayloadWithSmart().replace("\"model\": \"WDC WD40EFRX\"", "\"model\": \"WDC\\nINJECTED\"");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestAcceptsAgentMetrics() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "agent-metrics-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.ingestPayloadWithAgentMetrics()))
                .andExpect(status().isOk());
    }

    @Test
    void ingestAcceptsGzipEncodedBody() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "gzip-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .header("Content-Encoding", "gzip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gzip(TestFixtures.ingestPayloadWithAgentMetrics())))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Monitor-Features", "gzip-ingest,batch-ingest"));

        this.mockMvc.perform(get("/v1/servers/{serverId}", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentVersion").value("1.0.0"));
    }

    @Test
    void ingestRejectsMalformedGzipWithClientError() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "bad-gzip-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .header("Content-Encoding", "gzip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("definitely not gzip".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestRejectsUnsupportedContentEncoding() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "br-host");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .header("Content-Encoding", "br")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestFixtures.minimalIngestPayload()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void heartbeatAdvertisesSupportedFeatures() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "feature-host");

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString())))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Monitor-Features", "gzip-ingest,batch-ingest"));
    }

    private static byte[] gzip(String json) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    @Test
    void ingestAcceptsNonSemverAgentVersion() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "master-agent-host");
        String payload = TestFixtures.minimalIngestPayload().replace(
                "\"agentVersion\": \"1.0.0\"",
                "\"agentVersion\": \"master-abc1234\"");

        this.mockMvc.perform(post("/v1/servers/ingest")
                        .header("Authorization", TestAuthSupport.bearer(server.ingestToken().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/servers/{serverId}", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentVersion").value("master-abc1234"));
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
                .asString();

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(newToken)))
                .andExpect(status().isOk());

        this.mockMvc.perform(post("/v1/servers/heartbeat")
                        .header("Authorization", TestAuthSupport.bearer(originalToken)))
                .andExpect(status().isUnauthorized());
    }
}
