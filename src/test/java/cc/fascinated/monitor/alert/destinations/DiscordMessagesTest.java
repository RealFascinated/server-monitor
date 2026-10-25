package cc.fascinated.monitor.alert.destinations;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordMessagesTest {
    private static AlertNotification notification(String server, AlertTransition transition, int window) {
        return new AlertNotification(1, "High CPU", 7, server, "https://monitor.example/servers/7", "cpu",
                "CPU usage", Map.of("disk", "/"), AlertOperator.MORE_THAN, 90, 96.55, AlertUnit.PERCENT,
                TemperatureUnit.CELSIUS, AlertSeverity.CRITICAL, transition,
                window, transition == AlertTransition.RESOLVED ? Duration.ofMinutes(12) : null, Instant.EPOCH);
    }

    @Test
    void describesAFiringAlert() {
        String text = DiscordMessages.describe(notification("web-01", AlertTransition.FIRING, 300));

        assertThat(text).isEqualTo("**web-01**'s CPU usage is above 90%\nCPU usage: 96.55% (avg 5m) · disk: /");
    }

    @Test
    void describesARecoveryWithDuration() {
        AlertNotification resolved = new AlertNotification(1, null, 7, "web-01", "u", "cpu", "CPU usage", Map.of(),
                AlertOperator.MORE_THAN, 90, 42.1, AlertUnit.PERCENT, TemperatureUnit.CELSIUS, AlertSeverity.WARNING, AlertTransition.RESOLVED, 0,
                Duration.ofMinutes(12), Instant.EPOCH);

        assertThat(DiscordMessages.describe(resolved))
                .isEqualTo("**web-01**'s CPU usage is back below 90% — now 42.1%, was firing for 12m");
    }

    @Test
    void onOffMetricsReadAsHealthyOrUnhealthy() {
        AlertNotification firing = new AlertNotification(1, null, 7, "web-01", "u", "monitor_server_up", "Server status",
                Map.of(), AlertOperator.LESS_THAN, 1, 0, AlertUnit.STATE, TemperatureUnit.CELSIUS, AlertSeverity.CRITICAL,
                AlertTransition.FIRING, 0, null, Instant.EPOCH);
        AlertNotification resolved = new AlertNotification(1, null, 7, "web-01", "u", "monitor_server_up", "Server status",
                Map.of(), AlertOperator.LESS_THAN, 1, 1, AlertUnit.STATE, TemperatureUnit.CELSIUS, AlertSeverity.CRITICAL,
                AlertTransition.RESOLVED, 0, Duration.ofMinutes(12), Instant.EPOCH);

        assertThat(DiscordMessages.describe(firing)).isEqualTo("**web-01**'s Server status is unhealthy");
        assertThat(DiscordMessages.describe(resolved))
                .isEqualTo("**web-01**'s Server status is healthy again, was unhealthy for 12m");
    }

    @Test
    void serverNamesCannotInjectMarkdownOrMentions() {
        String escaped = DiscordMessages.escape("@everyone **bold** [x](y)");

        assertThat(escaped).doesNotContain("@everyone").contains("@​everyone").contains("\\*\\*bold\\*\\*");
        assertThat(DiscordMessages.payloads(List.of(notification("@everyone", AlertTransition.FIRING, 0))).getFirst())
                .contains("\"allowed_mentions\":{\"parse\":[]}");
    }

    @Test
    void burstsAreSplitIntoMessagesOfAtMostTenEmbeds() {
        List<AlertNotification> burst = IntStream.range(0, 25)
                .mapToObj(i -> notification("s" + i, AlertTransition.FIRING, 0)).toList();

        List<String> payloads = DiscordMessages.payloads(burst);

        assertThat(payloads).hasSize(3);
        assertThat(payloads.getFirst().split("\"title\"", -1)).hasSize(11);
    }

    @Test
    void webhookUrlsAreRestrictedToDiscord() {
        MonitorAlertProperties properties = new MonitorAlertProperties();

        assertThat(DiscordMessages.validateWebhookUrl("https://discord.com/api/webhooks/1/abc", properties))
                .isEqualTo("https://discord.com/api/webhooks/1/abc");
        for (String bad : List.of("http://discord.com/api/webhooks/1/abc", "https://evil.example/api/webhooks/1/abc",
                "https://discord.com.evil.example/api/webhooks/1/abc", "https://user@discord.com/api/webhooks/1/abc",
                "https://discord.com/other/1/abc", "https://discord.com/api/webhooks/", "not a url")) {
            assertThatThrownBy(() -> DiscordMessages.validateWebhookUrl(bad, properties))
                    .as(bad).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void maskedUrlsHideTheToken() {
        assertThat(DiscordMessages.maskWebhookUrl("https://discord.com/api/webhooks/123/secret"))
                .isEqualTo("https://discord.com/api/webhooks/123/••••");
    }

    @Test
    void temperaturesUseTheOwnersUnit() {
        AlertNotification hot = new AlertNotification(1, null, 7, "web-01", "u", "temp", "Temperature", Map.of(),
                AlertOperator.MORE_THAN, 85, 90, AlertUnit.TEMPERATURE, TemperatureUnit.FAHRENHEIT, AlertSeverity.WARNING,
                AlertTransition.FIRING, 0, null, Instant.EPOCH);

        assertThat(DiscordMessages.describe(hot)).isEqualTo("**web-01**'s Temperature is above 185°F\nTemperature: 194°F");
    }
}
