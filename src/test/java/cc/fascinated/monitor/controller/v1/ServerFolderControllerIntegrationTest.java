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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerFolderControllerIntegrationTest extends IntegrationTestBase {
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
    void managesFoldersAndAssignments() throws Exception {
        var createdFolder = this.mockMvc.perform(post("/v1/user/server-folders")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Production\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Production"))
                .andExpect(jsonPath("$.position").value(0))
                .andReturn();

        long folderId = this.objectMapper.readTree(createdFolder.getResponse().getContentAsString())
                .get("id")
                .asLong();

        var server = this.auth.createServer(this.bearerToken, "prod-db");

        this.mockMvc.perform(patch("/v1/servers/{serverId}/folder", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folderName\":\"Production\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderName").value("Production"));

        this.mockMvc.perform(get("/v1/user/server-folders")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Production"));

        this.mockMvc.perform(post("/v1/user/server-folders/{folderId}/rename", folderId)
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Prod\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prod"));

        this.mockMvc.perform(delete("/v1/user/server-folders/{folderId}", folderId)
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/v1/user/server-folders")
                        .header("Authorization", TestAuthSupport.bearer(this.bearerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
