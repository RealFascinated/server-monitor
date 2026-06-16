package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import cc.fascinated.monitor.support.TestFixtures;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password123";

    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.adminToken = this.auth.register(TestAuthSupport.uniqueEmail("admin"), PASSWORD).token();
        this.userToken = this.auth.register(TestAuthSupport.uniqueEmail("member"), PASSWORD).token();
    }

    @Test
    void adminCanQueryPlatformMetrics() throws Exception {
        long[] window = TestFixtures.metricWindow();

        this.mockMvc.perform(get("/v1/admin/metrics")
                        .header("Authorization", TestAuthSupport.bearer(this.adminToken))
                        .param("from", Long.toString(window[0]))
                        .param("to", Long.toString(window[1])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(window[0]))
                .andExpect(jsonPath("$.to").value(window[1]));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        long[] window = TestFixtures.metricWindow();

        this.mockMvc.perform(get("/v1/admin/metrics")
                        .header("Authorization", TestAuthSupport.bearer(this.userToken))
                        .param("from", Long.toString(window[0]))
                        .param("to", Long.toString(window[1])))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access required"));
    }
}
