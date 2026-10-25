package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.alerts")
public class MonitorAlertProperties {
    /** Notifications waiting for delivery; further ones are dropped (and counted) when full. */
    private int queueCapacity = 1000;

    /** Maximum alert rules one user may own. */
    private int maxRulesPerUser = 100;

    /** Maximum alert groups one user may own. */
    private int maxGroupsPerUser = 20;

    /** Maximum alert destinations one user may own. */
    private int maxDestinationsPerUser = 20;

    /** Notifications for the same destination arriving within this window are sent together. */
    private Duration coalesceWindow = Duration.ofSeconds(5);

    /** A series that stops reporting for this long has its alert state closed. */
    private Duration stateTtl = Duration.ofMinutes(10);

    /** How long per-server rule snapshots are trusted before being reloaded. */
    private Duration cacheTtl = Duration.ofSeconds(60);

    /** Timeout for one webhook request. */
    private Duration httpTimeout = Duration.ofSeconds(10);

    /** Longest averaging window a rule may use. */
    private Duration maxWindow = Duration.ofHours(24);

    /** Delivery attempts per notification batch before it is given up on. */
    private int maxDeliveryAttempts = 3;

    /** Hosts a Discord webhook URL may point at. */
    private List<String> allowedWebhookHosts = List.of("discord.com", "discordapp.com", "ptb.discord.com", "canary.discord.com");

    /** Refuse webhook URLs that are not https. */
    private boolean requireHttps = true;
}
