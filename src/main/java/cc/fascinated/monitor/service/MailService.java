package cc.fascinated.monitor.service;

import cc.fascinated.monitor.config.MonitorMailProperties;
import cc.fascinated.monitor.config.MonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class MailService {
    private final MonitorMailProperties mailProperties;
    private final MonitorProperties monitorProperties;
    @Nullable
    private final JavaMailSender mailSender;

    public MailService(
            MonitorMailProperties mailProperties,
            MonitorProperties monitorProperties,
            @Nullable JavaMailSender mailSender
    ) {
        this.mailProperties = mailProperties;
        this.monitorProperties = monitorProperties;
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        String baseUrl = this.monitorProperties.getWebsiteUrl().replaceAll("/+$", "");
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        if (!this.mailProperties.isEnabled() || this.mailSender == null) {
            log.info("Password reset link for {}: {}", to, resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(this.mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("Reset your Monitor password");
        message.setText("""
                You requested a password reset for your Monitor account.

                Reset your password using this link (valid for 1 hour):
                %s

                If you did not request this, you can ignore this email.
                """.formatted(resetUrl));
        this.mailSender.send(message);
    }

    public void sendServerInviteEmail(
            String to,
            String token,
            String serverName,
            String invitedByEmail
    ) {
        String baseUrl = this.monitorProperties.getWebsiteUrl().replaceAll("/+$", "");
        String inviteUrl = baseUrl
                + "/invites/accept?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email="
                + URLEncoder.encode(to, StandardCharsets.UTF_8);

        if (!this.mailProperties.isEnabled() || this.mailSender == null) {
            log.info("Server invite link for {}: {}", to, inviteUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(this.mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("You've been invited to %s on Monitor".formatted(serverName));
        message.setText("""
                %s invited you to join the server "%s" on Monitor.

                Accept the invite using this link (valid for 7 days):
                %s

                You'll receive viewer access once you accept.
                """.formatted(invitedByEmail, serverName, inviteUrl));
        this.mailSender.send(message);
    }
}
