package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSettingsControllerIntegrationTest extends IntegrationTestBase {
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
    void adminCanListAndUpdateSettings() throws Exception {
        this.mockMvc.perform(get("/v1/admin/settings")
                        .header("Authorization", TestAuthSupport.bearer(this.adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'registration_enabled')].value").value(true));

        this.mockMvc.perform(put("/v1/admin/settings/registration_enabled")
                        .header("Authorization", TestAuthSupport.bearer(this.adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("registration_enabled"))
                .andExpect(jsonPath("$.value").value(false));

        this.mockMvc.perform(put("/v1/admin/settings/registration_enabled")
                        .header("Authorization", TestAuthSupport.bearer(this.adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(true));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        this.mockMvc.perform(get("/v1/admin/settings")
                        .header("Authorization", TestAuthSupport.bearer(this.userToken)))
                .andExpect(status().isForbidden());
    }
}
