package cc.fascinated.monitor.model.domain.alert;

import cc.fascinated.monitor.alert.destinations.AlertDestinationTarget;

import java.util.List;

/** Delivers alerts to one destination. Receives a list so a burst can be coalesced into fewer messages. */
@FunctionalInterface
public interface AlertSender {
    void send(List<AlertNotification> alerts, AlertDestinationTarget target);
}
