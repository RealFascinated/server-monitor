package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void returnsPublicSettings() throws Exception {
        this.mockMvc.perform(get("/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("registration_enabled"))
                .andExpect(jsonPath("$[0].value").value(true));
    }
}
