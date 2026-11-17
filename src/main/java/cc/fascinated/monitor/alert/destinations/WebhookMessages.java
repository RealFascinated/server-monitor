package cc.fascinated.monitor.alert.destinations;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.util.Constants;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generic JSON webhook: one POST per delivery carrying every coalesced alert. */
@UtilityClass
public class WebhookMessages {

    public static String payload(List<AlertNotification> alerts) {
        List<Map<String, Object>> items = alerts.stream().map(WebhookMessages::alert).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", 1);
        body.put("alerts", items);
        return Constants.OBJECT_MAPPER.writeValueAsString(body);
    }

    private static Map<String, Object> alert(AlertNotification alert) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("transition", alert.transition().name());
        map.put("severity", alert.severity().name());
        map.put("ruleId", alert.ruleId());
        map.put("ruleName", alert.ruleName());
        map.put("serverId", alert.serverId());
        map.put("serverName", alert.serverName());
        map.put("serverUrl", alert.serverUrl());
        map.put("metric", alert.metric());
        map.put("metricLabel", alert.metricLabel());
        map.put("labels", alert.labels());
        map.put("operator", alert.operator().name());
        map.put("threshold", alert.threshold());
        map.put("value", alert.value());
        map.put("unit", alert.unit().name());
        map.put("windowSeconds", alert.windowSeconds());
        map.put("firedForSeconds", alert.firedFor() == null ? null : alert.firedFor().toSeconds());
        map.put("occurredAt", alert.occurredAt().toString());
        return map;
    }

    public static String validateUrl(String config, MonitorAlertProperties properties) {
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
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Webhook URL must have a host and no credentials");
        }
        return uri.toString();
    }

    /** The path and query can hold secrets, so only the origin is shown back. */
    public static String maskUrl(String config) {
        URI uri = URI.create(config);
        return "%s://%s/••••".formatted(uri.getScheme(), uri.getAuthority());
    }
}
