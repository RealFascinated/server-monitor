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

class UserPreferencesControllerIntegrationTest extends IntegrationTestBase {
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
    void returnsDefaultsAndPersistsUpdates() throws Exception {
        this.mockMvc.perform(get("/v1/user/preferences")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'metric_default_range')].value").value("7d"))
                .andExpect(jsonPath("$[?(@.key == 'sidebar_detailed_mode')].value").value(false));

        this.mockMvc.perform(put("/v1/user/preferences/sidebar_detailed_mode")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("sidebar_detailed_mode"))
                .andExpect(jsonPath("$.value").value(true));

        this.mockMvc.perform(get("/v1/user/preferences")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'sidebar_detailed_mode')].value").value(true));
    }

    @Test
    void rejectsUnknownPreference() throws Exception {
        this.mockMvc.perform(put("/v1/user/preferences/not_a_real_key")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        this.mockMvc.perform(get("/v1/user/preferences"))
                .andExpect(status().isUnauthorized());
    }
}
