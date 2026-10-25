package cc.fascinated.monitor.alert.destinations;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.util.Constants;
import cc.fascinated.monitor.util.DurationUtils;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class DiscordMessages {
    private static final int MAX_EMBEDS_PER_MESSAGE = 10;
    private static final int RESOLVED_COLOR = 0x22C55E;
    private static final String WEBHOOK_PATH_PREFIX = "/api/webhooks/";

    /** One JSON body per Discord message; a burst is split so no message carries more than ten embeds. */
    public static List<String> payloads(List<AlertNotification> alerts) {
        List<String> payloads = new ArrayList<>();
        for (int from = 0; from < alerts.size(); from += MAX_EMBEDS_PER_MESSAGE) {
            List<Map<String, Object>> embeds = alerts.subList(from, Math.min(alerts.size(), from + MAX_EMBEDS_PER_MESSAGE))
                    .stream()
                    .map(DiscordMessages::embed)
                    .toList();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("embeds", embeds);
            body.put("allowed_mentions", Map.of("parse", List.of()));
            payloads.add(Constants.OBJECT_MAPPER.writeValueAsString(body));
        }
        return payloads;
    }

    public static String describe(AlertNotification alert) {
        String name = "**" + escape(alert.serverName()) + "**'s " + alert.metricLabel();
        String threshold = alert.unit().format(alert.threshold(), alert.temperatureUnit());
        String value = alert.unit().format(alert.value(), alert.temperatureUnit());
        String labels = labelSuffix(alert.labels());
        if (alert.unit() == AlertUnit.STATE) {
            return describeState(alert, name, labels);
        }
        if (alert.transition() == AlertTransition.RESOLVED) {
            String duration = alert.firedFor() == null ? "" : ", was firing for " + DurationUtils.format(alert.firedFor());
            return "%s is %s %s%s, now %s%s".formatted(
                    name, alert.operator().recoveryPhrase(), threshold, labels.isEmpty() ? "" : " (" + labels + ")",
                    value, duration);
        }
        String window = alert.windowSeconds() > 0 ? " (avg " + DurationUtils.format(Duration.ofSeconds(alert.windowSeconds())) + ")" : "";
        String lead = switch (alert.transition()) {
            case REPEAT -> "is still ";
            case ESCALATED -> "has got worse, now ";
            default -> "is ";
        };
        String since = alert.transition() == AlertTransition.REPEAT && alert.firedFor() != null
                ? " · firing for " + DurationUtils.format(alert.firedFor()) : "";
        return "%s %s%s %s\n%s: %s%s%s%s".formatted(
                name, lead, alert.operator().phrase(), threshold,
                alert.metricLabel(), value, window, labels.isEmpty() ? "" : " · " + labels, since);
    }

    /** On/off metrics read "X is unhealthy" rather than "X is below 1". */
    private static String describeState(AlertNotification alert, String name, String labels) {
        String suffix = labels.isEmpty() ? "" : " (" + labels + ")";
        String duration = alert.firedFor() == null ? "" : DurationUtils.format(alert.firedFor());
        return switch (alert.transition()) {
            case RESOLVED -> "%s is healthy again%s%s".formatted(name, suffix,
                    duration.isEmpty() ? "" : ", was unhealthy for " + duration);
            case REPEAT -> "%s is still unhealthy%s%s".formatted(name, suffix,
                    duration.isEmpty() ? "" : " · for " + duration);
            case ESCALATED -> "%s has got worse%s".formatted(name, suffix);
            default -> "%s is unhealthy%s".formatted(name, suffix);
        };
    }

    public static String validateWebhookUrl(String config, MonitorAlertProperties properties) {
        URI uri;
        try {
            uri = URI.create(config.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Webhook URL is not a valid URL");
        }
        String scheme = uri.getScheme();
        boolean schemeOk = "https".equalsIgnoreCase(scheme) || (!properties.isRequireHttps() && "http".equalsIgnoreCase(scheme));
        if (!schemeOk) {
            throw new IllegalArgumentException("Webhook URL must use https");
        }
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null
                || properties.getAllowedWebhookHosts().stream().noneMatch(host::equalsIgnoreCase)) {
            throw new IllegalArgumentException("Webhook URL must point at a Discord webhook");
        }
        String path = uri.getPath();
        if (path == null || !path.startsWith(WEBHOOK_PATH_PREFIX) || path.length() <= WEBHOOK_PATH_PREFIX.length()) {
            throw new IllegalArgumentException("Webhook URL must look like https://discord.com/api/webhooks/<id>/<token>");
        }
        return uri.toString();
    }

    public static String maskWebhookUrl(String config) {
        URI uri = URI.create(config);
        String path = uri.getPath() == null ? "" : uri.getPath();
        String rest = path.length() > WEBHOOK_PATH_PREFIX.length() ? path.substring(WEBHOOK_PATH_PREFIX.length()) : "";
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        return "%s://%s%s%s/••••".formatted(uri.getScheme(), uri.getAuthority(), WEBHOOK_PATH_PREFIX, id);
    }

    /** Neutralises markdown and mention syntax in user-controlled text such as server names. */
    public static String escape(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ("\\*_~`|>#[]()".indexOf(c) >= 0) {
                builder.append('\\');
            }
            builder.append(c);
            if (c == '@') {
                builder.append('\u200B');
            }
        }
        return builder.toString();
    }

    private static Map<String, Object> embed(AlertNotification alert) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", title(alert));
        embed.put("url", alert.serverUrl());
        embed.put("description", describe(alert));
        embed.put("color", alert.transition() == AlertTransition.RESOLVED ? RESOLVED_COLOR : alert.severity().color());
        embed.put("timestamp", alert.occurredAt().toString());
        return embed;
    }

    private static String title(AlertNotification alert) {
        String prefix = switch (alert.transition()) {
            case FIRING -> alert.severity().label() + " · Firing";
            case REPEAT -> alert.severity().label() + " · Still firing";
            case ESCALATED -> alert.severity().label() + " · Got worse";
            case RESOLVED -> "Resolved";
        };
        return alert.ruleName() == null || alert.ruleName().isBlank() ? prefix : prefix + " · " + alert.ruleName();
    }

    private static String labelSuffix(Map<String, String> labels) {
        if (labels.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>(labels.size());
        labels.forEach((key, value) -> parts.add(key + ": " + escape(value)));
        return String.join(", ", parts);
    }
}
