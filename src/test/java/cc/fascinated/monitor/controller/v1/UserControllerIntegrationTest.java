package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends IntegrationTestBase {
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
    void listsServersForAuthenticatedUser() throws Exception {
        this.mockMvc.perform(get("/v1/user/servers")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        this.auth.createServer(this.bearerToken, "web-1");

        this.mockMvc.perform(get("/v1/user/servers")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serverName").value("web-1"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        this.mockMvc.perform(get("/v1/user/servers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordAndInvalidatesOtherSessions() throws Exception {
        String email = TestAuthSupport.uniqueEmail("password-change");
        var firstSession = this.auth.register(email, PASSWORD);
        var secondSession = this.auth.login(email, PASSWORD);

        this.mockMvc.perform(patch("/v1/user/password")
                        .header("Authorization", TestAuthSupport.bearer(firstSession.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"new-password-99"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/auth/@me")
                        .header("Authorization", TestAuthSupport.bearer(firstSession.token())))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/auth/@me")
                        .header("Authorization", TestAuthSupport.bearer(secondSession.token())))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"new-password-99"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void returnsServerMetricsForAccessibleServer() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "metrics-host");
        long[] window = TestFixtures.metricWindow();

        this.mockMvc.perform(get("/v1/user/servers/{serverId}/metrics", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .param("from", Long.toString(window[0]))
                        .param("to", Long.toString(window[1])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(server.serverId()))
                .andExpect(jsonPath("$.from").value(window[0]))
                .andExpect(jsonPath("$.to").value(window[1]));
    }

    @Test
    void rejectsInvalidMetricWindow() throws Exception {
        var server = this.auth.createServer(this.bearerToken, "metrics-host");
        long now = TestFixtures.metricWindow()[1];

        this.mockMvc.perform(get("/v1/user/servers/{serverId}/metrics", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .param("from", Long.toString(now))
                        .param("to", Long.toString(now - 60)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before to"));
    }
}
