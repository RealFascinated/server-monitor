package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.mail")
public class MonitorMailProperties {
    private boolean enabled = false;
    private String host = "localhost";
    private int port = 1025;
    private String username = "";
    private String password = "";
    private boolean tls = false;
    private boolean ssl = false;
    private String from = "Monitor <noreply@localhost>";
}
