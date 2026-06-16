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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerAccessIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "password123";

    @Autowired
    private ObjectMapper objectMapper;

    private TestAuthSupport auth;
    private String ownerToken;
    private String memberToken;
    private String memberEmail;

    @BeforeEach
    void setUp() throws Exception {
        this.auth = new TestAuthSupport(this.mockMvc, this.objectMapper);
        this.ownerToken = this.auth.register(TestAuthSupport.uniqueEmail("owner"), PASSWORD).token();
        this.memberEmail = TestAuthSupport.uniqueEmail("member");
        this.memberToken = this.auth.register(this.memberEmail, PASSWORD).token();
    }

    @Test
    void inviteAcceptAndListMembers() throws Exception {
        var server = this.auth.createServer(this.ownerToken, "shared-host");

        var invite = this.mockMvc.perform(post("/v1/servers/{serverId}/members/invite", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(this.memberEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(this.memberEmail))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andReturn();

        long inviteId = this.objectMapper.readTree(invite.getResponse().getContentAsString())
                .get("inviteId")
                .asLong();
        String inviteToken = this.objectMapper.readTree(invite.getResponse().getContentAsString())
                .get("token")
                .asText();

        this.mockMvc.perform(get("/v1/user/invites/preview")
                        .header("Authorization", TestAuthSupport.bearer(this.memberToken))
                        .param("token", inviteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverName").value("shared-host"))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        this.mockMvc.perform(get("/v1/servers/{serverId}/members", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.email").exists())
                .andExpect(jsonPath("$.pendingInvites.length()").value(1))
                .andExpect(jsonPath("$.pendingInvites[0].email").value(this.memberEmail));

        this.mockMvc.perform(get("/v1/user/invites")
                        .header("Authorization", TestAuthSupport.bearer(this.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serverName").value("shared-host"));

        this.mockMvc.perform(post("/v1/user/invites/{inviteId}/accept", inviteId)
                        .header("Authorization", TestAuthSupport.bearer(this.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverName").value("shared-host"))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        this.mockMvc.perform(get("/v1/user/servers")
                        .header("Authorization", TestAuthSupport.bearer(this.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("VIEWER"));

        this.mockMvc.perform(get("/v1/servers/{serverId}/members", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.pendingInvites.length()").value(0));
    }

    @Test
    void ownerCanRevokePendingInvite() throws Exception {
        var server = this.auth.createServer(this.ownerToken, "revoke-host");
        String inviteeEmail = TestAuthSupport.uniqueEmail("invitee");

        var invite = this.mockMvc.perform(post("/v1/servers/{serverId}/members/invite", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(inviteeEmail)))
                .andExpect(status().isOk())
                .andReturn();

        long inviteId = this.objectMapper.readTree(invite.getResponse().getContentAsString())
                .get("inviteId")
                .asLong();

        this.mockMvc.perform(delete("/v1/servers/{serverId}/invites/{inviteId}", server.serverId(), inviteId)
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/v1/servers/{serverId}/members", server.serverId())
                        .header("Authorization", TestAuthSupport.bearer(this.ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingInvites.length()").value(0));
    }
}
