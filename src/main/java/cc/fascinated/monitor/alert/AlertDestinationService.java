package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.alert.destinations.AlertDeliveryException;
import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.config.MonitorProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.domain.alert.AlertNotification;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertTransition;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import cc.fascinated.monitor.model.dto.request.alert.AlertDestinationRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertDestinationUpdateRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertDestinationResponse;
import cc.fascinated.monitor.model.persistance.AlertDestinationRow;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.AlertDestinationRepository;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.service.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AlertDestinationService {
    private final AlertDestinationRepository destinationRepository;
    private final AlertGroupRepository groupRepository;
    private final EncryptionService encryptionService;
    private final MonitorAlertProperties properties;
    private final MonitorProperties monitorProperties;
    private final AlertDispatcher dispatcher;
    private final AlertStateService stateService;

    public AlertDestinationService(AlertDestinationRepository destinationRepository, AlertGroupRepository groupRepository,
                                   EncryptionService encryptionService, MonitorAlertProperties properties,
                                   MonitorProperties monitorProperties, AlertDispatcher dispatcher,
                                   AlertStateService stateService) {
        this.destinationRepository = destinationRepository;
        this.groupRepository = groupRepository;
        this.encryptionService = encryptionService;
        this.properties = properties;
        this.monitorProperties = monitorProperties;
        this.dispatcher = dispatcher;
        this.stateService = stateService;
    }

    public List<AlertDestinationResponse> list(UserRow user) {
        return this.destinationRepository.findByUserIdOrderByIdAsc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertDestinationResponse create(UserRow user, AlertDestinationRequest request) {
        if (this.destinationRepository.countByUserId(user.getId()) >= this.properties.getMaxDestinationsPerUser()) {
            throw new ConflictException("You can have at most %d alert destinations"
                    .formatted(this.properties.getMaxDestinationsPerUser()));
        }
        String config = validateConfig(request.type(), request.config());
        AlertDestinationRow row = new AlertDestinationRow(user.getId(), request.name().trim(), request.type(),
                this.encryptionService.encrypt(config), Instant.now());
        boolean first = this.destinationRepository.countByUserId(user.getId()) == 0;
        AlertDestinationRow saved = this.destinationRepository.save(row);
        if (first && this.groupRepository.attachDestination(user.getId(), saved.getId()) > 0) {
            this.stateService.invalidateAll();
        }
        return toResponse(saved);
    }

    @Transactional
    public AlertDestinationResponse update(UserRow user, long id, AlertDestinationUpdateRequest request) {
        AlertDestinationRow row = require(user, id);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Name must not be empty");
            }
            row.setName(request.name().trim());
        }
        if (request.config() != null) {
            row.setConfigEncrypted(this.encryptionService.encrypt(validateConfig(row.getType(), request.config())));
        }
        return toResponse(row);
    }

    @Transactional
    public void delete(UserRow user, long id, boolean force) {
        AlertDestinationRow row = require(user, id);
        List<AlertGroupRow> groups = this.groupRepository.findByDestinationId(id);
        if (!groups.isEmpty() && !force) {
            throw new ConflictException("This destination is used by %d alert group(s). Deleting it stops them notifying anyone."
                    .formatted(groups.size()));
        }
        groups.forEach(group -> group.setDestinationId(null));
        this.destinationRepository.delete(row);
        this.stateService.invalidateAll();
    }

    /** Sends one sample message, without retries, so the user sees a real failure straight away. */
    public void test(UserRow user, long id) {
        AlertDestinationRow row = require(user, id);
        AlertNotification sample = new AlertNotification(0, "Test alert", 0, "Test server", this.monitorProperties.getWebsiteUrl(),
                "cpu_usage", "CPU usage", Map.of(), AlertOperator.MORE_THAN, 90, 96.55, AlertUnit.PERCENT,
                TemperatureUnit.CELSIUS, AlertSeverity.WARNING, AlertTransition.FIRING, 300, null, Instant.now());
        try {
            row.getType().send(List.of(sample), this.dispatcher.targetFor(row));
        } catch (AlertDeliveryException ex) {
            throw new BadRequestException("The test message could not be delivered: " + ex.getMessage());
        }
    }

    private AlertDestinationRow require(UserRow user, long id) {
        return this.destinationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Alert destination not found"));
    }

    private String validateConfig(cc.fascinated.monitor.model.domain.alert.AlertDestination type, String config) {
        try {
            return type.validateConfig(config.trim(), this.properties);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private AlertDestinationResponse toResponse(AlertDestinationRow row) {
        String masked = row.getType().mask(this.encryptionService.decrypt(row.getConfigEncrypted()));
        return AlertDestinationResponse.from(row, masked, this.groupRepository.countByDestinationId(row.getId()));
    }
}
