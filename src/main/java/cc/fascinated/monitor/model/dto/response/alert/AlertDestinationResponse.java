package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.model.domain.alert.AlertDestination;
import cc.fascinated.monitor.model.persistance.AlertDestinationRow;

/** The stored webhook URL is a secret, so only a masked form is ever returned. */
public record AlertDestinationResponse(long id, String name, AlertDestination type, String maskedUrl, long groupCount) {

    public static AlertDestinationResponse from(AlertDestinationRow row, String maskedUrl, long groupCount) {
        return new AlertDestinationResponse(row.getId(), row.getName(), row.getType(), maskedUrl, groupCount);
    }
}
