package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.agent")
public class AgentUpdateProperties {
    private String githubOwner = "RealFascinated";
    private String githubRepo = "server-monitor";
    private String tagPrefix = "agent/v";
    private Duration releaseCacheTtl = Duration.ofMinutes(5);
}
