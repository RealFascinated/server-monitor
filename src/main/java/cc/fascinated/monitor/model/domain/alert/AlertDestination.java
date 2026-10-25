package cc.fascinated.monitor.model.domain.alert;

import cc.fascinated.monitor.alert.destinations.DiscordMessages;
import cc.fascinated.monitor.alert.destinations.AlertDestinationTarget;
import cc.fascinated.monitor.config.MonitorAlertProperties;

import java.util.List;

public enum AlertDestination {
    DISCORD_WEBHOOK(
            (alerts, target) -> DiscordMessages.payloads(alerts).forEach(target::postJson),
            DiscordMessages::validateWebhookUrl,
            DiscordMessages::maskWebhookUrl
    );

    private final AlertSender sender;
    private final ConfigValidator validator;
    private final ConfigMasker masker;

    AlertDestination(AlertSender sender, ConfigValidator validator, ConfigMasker masker) {
        this.sender = sender;
        this.validator = validator;
        this.masker = masker;
    }

    public void send(List<AlertNotification> alerts, AlertDestinationTarget target) {
        this.sender.send(alerts, target);
    }

    /** @return the normalised config to store; throws {@link IllegalArgumentException} when invalid */
    public String validateConfig(String config, MonitorAlertProperties properties) {
        return this.validator.validate(config, properties);
    }

    /** A form of the stored config that is safe to show back to the user. */
    public String mask(String config) {
        return this.masker.mask(config);
    }

    @FunctionalInterface
    private interface ConfigValidator {
        String validate(String config, MonitorAlertProperties properties);
    }

    @FunctionalInterface
    private interface ConfigMasker {
        String mask(String config);
    }
}
