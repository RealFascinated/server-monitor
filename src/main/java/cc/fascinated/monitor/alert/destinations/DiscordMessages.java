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
import java.util.Optional;
import java.util.stream.Collectors;

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

    /** The embed title: what happened, in a few words. Details live in the embed fields. */
    public static String headline(AlertNotification alert) {
        String metric = alert.metricLabel();
        if (alert.unit() == AlertUnit.STATE) {
            return switch (alert.transition()) {
                case FIRING -> metric + " is unhealthy";
                case REPEAT -> metric + " is still unhealthy";
                case ESCALATED -> metric + " got worse";
                case RESOLVED -> metric + " is healthy again";
            };
        }
        String threshold = alert.unit().format(alert.threshold(), alert.temperatureUnit());
        return switch (alert.transition()) {
            case FIRING -> "%s is %s %s".formatted(metric, alert.operator().phrase(), threshold);
            case REPEAT -> "%s is still %s %s".formatted(metric, alert.operator().phrase(), threshold);
            case ESCALATED -> metric + " got worse";
            case RESOLVED -> "%s is %s %s".formatted(metric, alert.operator().recoveryPhrase(), threshold);
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
        boolean resolved = alert.transition() == AlertTransition.RESOLVED;
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("author", Map.of("name", status(alert)));
        embed.put("title", headline(alert));
        embed.put("url", alert.serverUrl());
        embed.put("color", resolved ? RESOLVED_COLOR : alert.severity().color());
        embed.put("fields", fields(alert));
        if (alert.ruleName() != null && !alert.ruleName().isBlank()) {
            embed.put("footer", Map.of("text", alert.ruleName()));
        }
        embed.put("timestamp", alert.occurredAt().toString());
        return embed;
    }

    private static String status(AlertNotification alert) {
        return switch (alert.transition()) {
            case FIRING -> alert.severity().label() + " · Firing";
            case REPEAT -> alert.severity().label() + " · Still firing";
            case ESCALATED -> alert.severity().label() + " · Got worse";
            case RESOLVED -> "Resolved";
        };
    }

    private static List<Map<String, Object>> fields(AlertNotification alert) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("Server", escape(alert.serverName())));
        series(alert.labels()).ifPresent(series -> fields.add(field(series.name(), series.value())));
        if (alert.unit() != AlertUnit.STATE) {
            boolean resolved = alert.transition() == AlertTransition.RESOLVED;
            String window = alert.windowSeconds() > 0
                    ? " (avg " + DurationUtils.format(Duration.ofSeconds(alert.windowSeconds())) + ")" : "";
            fields.add(field((resolved ? "Now" : "Value") + window,
                    alert.unit().format(alert.value(), alert.temperatureUnit())));
            fields.add(field("Threshold", alert.operator().phrase() + " "
                    + alert.unit().format(alert.threshold(), alert.temperatureUnit())));
        }
        if (alert.firedFor() != null) {
            String name = alert.transition() == AlertTransition.RESOLVED ? "Was firing for" : "Firing for";
            fields.add(field(name, DurationUtils.format(alert.firedFor())));
        }
        return fields;
    }

    private static Map<String, Object> field(String name, String value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", true);
        return field;
    }

    private record Series(String name, String value) {}

    /** A single label reads as "Disk: /home"; several are joined under "Series", as the web UI does. */
    private static Optional<Series> series(Map<String, String> labels) {
        if (labels.isEmpty()) {
            return Optional.empty();
        }
        if (labels.size() == 1) {
            Map.Entry<String, String> only = labels.entrySet().iterator().next();
            String key = only.getKey();
            return Optional.of(new Series(Character.toUpperCase(key.charAt(0)) + key.substring(1), escape(only.getValue())));
        }
        return Optional.of(new Series("Series", labels.values().stream().map(DiscordMessages::escape)
                .collect(Collectors.joining(" · "))));
    }
}
