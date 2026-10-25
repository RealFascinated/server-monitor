package cc.fascinated.monitor.alert.destinations;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import cc.fascinated.monitor.util.Constants;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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

    private static JsonNode embedOf(AlertNotification alert) throws Exception {
        return Constants.OBJECT_MAPPER.readTree(DiscordMessages.payloads(List.of(alert)).getFirst()).get("embeds").get(0);
    }

    private static Map<String, String> fieldsOf(JsonNode embed) {
        Map<String, String> fields = new LinkedHashMap<>();
        embed.get("fields").forEach(field -> fields.put(field.get("name").asString(), field.get("value").asString()));
        return fields;
    }

    @Test
    void firingEmbedHasAHeadlineAndDetailFields() throws Exception {
        JsonNode embed = embedOf(notification("web-01", AlertTransition.FIRING, 300));

        assertThat(embed.get("author").get("name").asString()).isEqualTo("Critical · Firing");
        assertThat(embed.get("title").asString()).isEqualTo("CPU usage is above 90%");
        assertThat(embed.get("footer").get("text").asString()).isEqualTo("High CPU");
        assertThat(fieldsOf(embed)).containsExactly(
                Map.entry("Server", "web-01"),
                Map.entry("Disk", "/"),
                Map.entry("Value (avg 5m)", "96.55%"),
                Map.entry("Threshold", "above 90%"));
    }

    @Test
    void recoveryEmbedShowsHowLongItFired() throws Exception {
        AlertNotification resolved = new AlertNotification(1, null, 7, "web-01", "u", "cpu", "CPU usage", Map.of(),
                AlertOperator.MORE_THAN, 90, 42.1, AlertUnit.PERCENT, TemperatureUnit.CELSIUS, AlertSeverity.WARNING, AlertTransition.RESOLVED, 0,
                Duration.ofMinutes(12), Instant.EPOCH);
        JsonNode embed = embedOf(resolved);

        assertThat(embed.get("author").get("name").asString()).isEqualTo("Resolved");
        assertThat(embed.get("title").asString()).isEqualTo("CPU usage is back below 90%");
        assertThat(embed.has("footer")).as("no footer without a rule name").isFalse();
        assertThat(fieldsOf(embed)).containsExactly(
                Map.entry("Server", "web-01"),
                Map.entry("Now", "42.1%"),
                Map.entry("Threshold", "above 90%"),
                Map.entry("Was firing for", "12m"));
    }

    @Test
    void escalationHeadlineDoesNotRepeatTheThreshold() throws Exception {
        JsonNode embed = embedOf(notification("web-01", AlertTransition.ESCALATED, 0));

        assertThat(embed.get("author").get("name").asString()).isEqualTo("Critical · Got worse");
        assertThat(embed.get("title").asString()).isEqualTo("CPU usage got worse");
        assertThat(fieldsOf(embed)).containsEntry("Value", "96.55%");
    }

    @Test
    void severalLabelsAreJoinedUnderSeries() throws Exception {
        AlertNotification drive = new AlertNotification(1, null, 7, "web-01", "u", "crc", "UDMA CRC errors",
                new java.util.LinkedHashMap<>(Map.of("device", "sda")) {{ put("model", "WDC WD40EFRX"); }},
                AlertOperator.MORE_THAN, 0, 12, AlertUnit.COUNT, TemperatureUnit.CELSIUS, AlertSeverity.WARNING,
                AlertTransition.FIRING, 0, null, Instant.EPOCH);

        assertThat(fieldsOf(embedOf(drive))).containsEntry("Series", "sda · WDC WD40EFRX");
    }

    @Test
    void onOffMetricsReadAsHealthyOrUnhealthy() throws Exception {
        AlertNotification firing = new AlertNotification(1, null, 7, "web-01", "u", "monitor_server_up", "Server status",
                Map.of(), AlertOperator.LESS_THAN, 1, 0, AlertUnit.STATE, TemperatureUnit.CELSIUS, AlertSeverity.CRITICAL,
                AlertTransition.FIRING, 0, null, Instant.EPOCH);
        AlertNotification resolved = new AlertNotification(1, null, 7, "web-01", "u", "monitor_server_up", "Server status",
                Map.of(), AlertOperator.LESS_THAN, 1, 1, AlertUnit.STATE, TemperatureUnit.CELSIUS, AlertSeverity.CRITICAL,
                AlertTransition.RESOLVED, 0, Duration.ofMinutes(12), Instant.EPOCH);

        assertThat(embedOf(firing).get("title").asString()).isEqualTo("Server status is unhealthy");
        assertThat(fieldsOf(embedOf(firing))).containsOnlyKeys("Server");
        assertThat(embedOf(resolved).get("title").asString()).isEqualTo("Server status is healthy again");
        assertThat(fieldsOf(embedOf(resolved))).containsEntry("Was firing for", "12m");
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
    void temperaturesUseTheOwnersUnit() throws Exception {
        AlertNotification hot = new AlertNotification(1, null, 7, "web-01", "u", "temp", "Temperature", Map.of(),
                AlertOperator.MORE_THAN, 85, 90, AlertUnit.TEMPERATURE, TemperatureUnit.FAHRENHEIT, AlertSeverity.WARNING,
                AlertTransition.FIRING, 0, null, Instant.EPOCH);

        assertThat(embedOf(hot).get("title").asString()).isEqualTo("Temperature is above 185°F");
        assertThat(fieldsOf(embedOf(hot))).containsEntry("Value", "194°F");
    }
}
