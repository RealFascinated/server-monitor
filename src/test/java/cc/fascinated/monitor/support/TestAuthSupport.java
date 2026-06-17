package cc.fascinated.monitor.support;

import cc.fascinated.monitor.model.dto.response.auth.AuthTokenResponse;
import cc.fascinated.monitor.model.dto.response.server.CreatedServerResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAuthSupport {
    public static final String TEST_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestAuthSupport(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public AuthTokenResponse register(String email, String password) throws Exception {
        MvcResult result = this.mockMvc.perform(post("/v1/auth/register")
                        .header("User-Agent", TEST_USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return this.objectMapper.readValue(result.getResponse().getContentAsString(), AuthTokenResponse.class);
    }

    public AuthTokenResponse login(String email, String password) throws Exception {
        MvcResult result = this.mockMvc.perform(post("/v1/auth/login")
                        .header("User-Agent", TEST_USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return this.objectMapper.readValue(result.getResponse().getContentAsString(), AuthTokenResponse.class);
    }

    public CreatedServerResponse createServer(String bearerToken, String name) throws Exception {
        MvcResult result = this.mockMvc.perform(post("/v1/servers/create")
                        .header("Authorization", bearer(bearerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return this.objectMapper.readValue(result.getResponse().getContentAsString(), CreatedServerResponse.class);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
