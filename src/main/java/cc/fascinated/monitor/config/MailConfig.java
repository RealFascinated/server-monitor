package cc.fascinated.monitor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MonitorMailProperties.class)
public class MailConfig {
    @Bean
    @ConditionalOnProperty(prefix = "monitor.mail", name = "enabled", havingValue = "true")
    JavaMailSender javaMailSender(MonitorMailProperties properties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(properties.getHost());
        mailSender.setPort(properties.getPort());
        mailSender.setUsername(properties.getUsername());
        mailSender.setPassword(properties.getPassword());

        boolean auth = !properties.getUsername().isBlank();
        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.smtp.auth", Boolean.toString(auth));
        javaMailProperties.put("mail.smtp.starttls.enable", Boolean.toString(properties.isTls()));
        javaMailProperties.put("mail.smtp.starttls.required", Boolean.toString(properties.isTls()));
        javaMailProperties.put("mail.smtp.ssl.enable", Boolean.toString(properties.isSsl()));

        return mailSender;
    }
}
