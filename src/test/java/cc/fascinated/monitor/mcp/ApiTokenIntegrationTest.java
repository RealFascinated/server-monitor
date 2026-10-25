package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiTokenIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password-1234";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestAuthSupport auth;
    private String session;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.session = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();
    }

    private JsonNode create(String bearer, Map<String, Object> body, int expectedStatus) throws Exception {
        String response = this.mockMvc.perform(post("/v1/user/api-tokens")
                        .header("Authorization", TestAuthSupport.bearer(bearer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : this.objectMapper.readTree(response);
    }

    @Test
    void createReturnsTheRawTokenOnceAndStoresOnlyItsHash() throws Exception {
        JsonNode created = create(this.session, Map.of("name", "claude", "scopes", List.of("servers:read")), 200);

        String raw = created.get("token").asString();
        assertThat(raw).startsWith("smk_");
        assertThat(created.get("info").get("prefix").asString()).isEqualTo(raw.substring(0, 12));
        String stored = this.jdbcTemplate.queryForObject("SELECT token_hash FROM api_tokens", String.class);
        assertThat(stored).isNotEqualTo(raw).hasSize(64);

        this.mockMvc.perform(get("/v1/user/api-tokens")
                        .header("Authorization", TestAuthSupport.bearer(this.session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("claude"))
                .andExpect(jsonPath("$[0].scopes[0]").value("servers:read"))
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    void rejectsUnknownAndMissingScopes() throws Exception {
        create(this.session, Map.of("name", "x", "scopes", List.of("servers:admin")), 400);
        create(this.session, Map.of("name", "x", "scopes", List.of()), 400);
        create(this.session, Map.of("name", " ", "scopes", List.of("servers:read")), 400);
    }

    @Test
    void listsTheAvailableScopes() throws Exception {
        this.mockMvc.perform(get("/v1/user/api-tokens/scopes")
                        .header("Authorization", TestAuthSupport.bearer(this.session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].id").value("alerts:write"));
    }

    @Test
    void revokeRemovesTheTokenAndOnlyTheOwnerCanRevoke() throws Exception {
        JsonNode created = create(this.session, Map.of("name", "claude", "scopes", List.of("servers:read")), 200);
        long id = created.get("info").get("id").asLong();
        String other = this.auth.register(TestAuthSupport.uniqueEmail(), PASSWORD).token();

        this.mockMvc.perform(delete("/v1/user/api-tokens/{id}", id)
                        .header("Authorization", TestAuthSupport.bearer(other)))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(delete("/v1/user/api-tokens/{id}", id)
                        .header("Authorization", TestAuthSupport.bearer(this.session)))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/v1/user/api-tokens")
                        .header("Authorization", TestAuthSupport.bearer(this.session)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anApiTokenCannotUseTheRestApiOrManageTokens() throws Exception {
        String apiToken = create(this.session, Map.of("name", "claude",
                "scopes", List.of("servers:read", "alerts:write")), 200).get("token").asString();

        this.mockMvc.perform(get("/v1/user/servers")
                        .header("Authorization", TestAuthSupport.bearer(apiToken)))
                .andExpect(status().isUnauthorized());
        create(apiToken, Map.of("name", "child", "scopes", List.of("servers:read")), 401);
        this.mockMvc.perform(get("/v1/user/api-tokens")
                        .header("Authorization", TestAuthSupport.bearer(apiToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void capsTheNumberOfTokensPerUser() throws Exception {
        for (int i = 0; i < 20; i++) {
            create(this.session, Map.of("name", "t" + i, "scopes", List.of("servers:read")), 200);
        }
        create(this.session, Map.of("name", "one-too-many", "scopes", List.of("servers:read")), 409);
    }
}
