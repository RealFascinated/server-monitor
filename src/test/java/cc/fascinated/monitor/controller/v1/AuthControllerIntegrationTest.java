package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.support.IntegrationTestBase;
import cc.fascinated.monitor.support.TestAuthSupport;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password123";

    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;

    @BeforeEach
    void setUp() {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
    }

    @Test
    void registerLoginMeAndLogout() throws Exception {
        String email = TestAuthSupport.uniqueEmail();
        var registered = this.auth.register(email, PASSWORD);

        this.mockMvc.perform(get("/v1/auth/@me")
                        .header("Authorization", TestAuthSupport.bearer(registered.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        var loggedIn = this.auth.login(email, PASSWORD);
        this.mockMvc.perform(get("/v1/auth/@me")
                        .header("Authorization", TestAuthSupport.bearer(loggedIn.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        this.mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", TestAuthSupport.bearer(loggedIn.token())))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/auth/@me")
                        .header("Authorization", TestAuthSupport.bearer(loggedIn.token())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = TestAuthSupport.uniqueEmail();
        this.auth.register(email, PASSWORD);

        this.mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        String email = TestAuthSupport.uniqueEmail();
        this.auth.register(email, PASSWORD);

        this.mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meRequiresAuthorization() throws Exception {
        this.mockMvc.perform(get("/v1/auth/@me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordAlwaysReturnsSuccess() throws Exception {
        String email = TestAuthSupport.uniqueEmail();
        this.auth.register(email, PASSWORD);

        this.mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }
}
