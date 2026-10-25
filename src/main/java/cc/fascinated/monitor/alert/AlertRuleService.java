package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.dto.request.alert.AlertRuleRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertRuleResponse;
import cc.fascinated.monitor.model.persistance.AlertDestinationRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.AlertDestinationRepository;
import cc.fascinated.monitor.repository.AlertRuleEventStat;
import cc.fascinated.monitor.repository.AlertEventRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AlertRuleService {
    private static final int MIN_REPEAT_SECONDS = 60;
    private static final int MAX_REPEAT_SECONDS = 30 * 24 * 3600;

    private final AlertRuleRepository ruleRepository;
    private final AlertDestinationRepository destinationRepository;
    private final AlertEventRepository eventRepository;
    private final AlertGroupRepository groupRepository;
    private final AlertRuleValidator validator;
    private final AlertStateService stateService;
    private final MonitorAlertProperties properties;

    public AlertRuleService(AlertRuleRepository ruleRepository, AlertDestinationRepository destinationRepository,
                            AlertEventRepository eventRepository, AlertGroupRepository groupRepository,
                            AlertRuleValidator validator, AlertStateService stateService,
                            MonitorAlertProperties properties) {
        this.ruleRepository = ruleRepository;
        this.destinationRepository = destinationRepository;
        this.eventRepository = eventRepository;
        this.groupRepository = groupRepository;
        this.validator = validator;
        this.stateService = stateService;
        this.properties = properties;
    }

    /** The caller's rules, optionally only those of one group. */
    @Transactional(readOnly = true)
    public List<AlertRuleResponse> list(UserRow user, @Nullable Long groupId) {
        if (groupId == null) {
            return toResponses(this.ruleRepository.findByUserIdOrderByIdDesc(user.getId()));
        }
        requireGroup(user, groupId);
        return toResponses(this.ruleRepository.findByGroupIdOrderByIdDesc(groupId));
    }

    @Transactional(readOnly = true)
    public AlertRuleResponse get(UserRow user, long id) {
        return toResponses(List.of(require(user, id))).getFirst();
    }

    @Transactional
    public AlertRuleResponse create(UserRow user, AlertRuleRequest request) {
        if (this.ruleRepository.countByUserId(user.getId()) >= this.properties.getMaxRulesPerUser()) {
            throw new ConflictException("You can have at most %d alert rules".formatted(this.properties.getMaxRulesPerUser()));
        }
        AlertGroupRow group = requireGroup(user, request.groupId());
        AlertRuleRow row = new AlertRuleRow(user.getId(), group.getId(), Instant.now());
        apply(user, row, request);
        row.setEnabled(request.enabled() == null || request.enabled());
        AlertRuleRow saved = this.ruleRepository.save(row);
        invalidate(saved);
        return toResponses(List.of(saved)).getFirst();
    }

    @Transactional
    public AlertRuleResponse update(UserRow user, long id, AlertRuleRequest request) {
        AlertRuleRow row = require(user, id);
        if (!Objects.equals(row.getGroupId(), request.groupId())) {
            throw new BadRequestException("An alert cannot move to another group, create a new alert instead");
        }
        String before = evaluationFingerprint(row);
        apply(user, row, request);
        if (request.enabled() != null) {
            row.setEnabled(request.enabled());
        }
        this.ruleRepository.flush();
        if (!before.equals(evaluationFingerprint(row)) || !row.isEnabled()) {
            this.stateService.retire(List.of(row.getId()));
        }
        invalidate(row);
        return toResponses(List.of(row)).getFirst();
    }

    @Transactional
    public AlertRuleResponse setEnabled(UserRow user, long id, boolean enabled) {
        AlertRuleRow row = require(user, id);
        row.setEnabled(enabled);
        if (!enabled) {
            this.stateService.retire(List.of(row.getId()));
        }
        invalidate(row);
        return toResponses(List.of(row)).getFirst();
    }

    @Transactional
    public void delete(UserRow user, long id) {
        AlertRuleRow row = require(user, id);
        this.stateService.retire(List.of(row.getId()));
        this.ruleRepository.delete(row);
        invalidate(row);
    }

    private void apply(UserRow user, AlertRuleRow row, AlertRuleRequest request) {
        AlertRuleValidator.Definition definition = this.validator.validate(request.metric(), request.labelFilter(),
                request.operator(), request.threshold(), request.recoveryThreshold(), request.windowSeconds());
        Long destinationId = request.destinationId() == null ? null : this.destinationRepository
                .findByIdAndUserId(request.destinationId(), user.getId())
                .orElseThrow(() -> new BadRequestException("Choose one of your own alert destinations")).getId();
        Integer repeat = request.repeatIntervalSeconds();
        if (repeat != null && (repeat < MIN_REPEAT_SECONDS || repeat > MAX_REPEAT_SECONDS)) {
            throw new BadRequestException("Repeat interval must be between %d seconds and 30 days".formatted(MIN_REPEAT_SECONDS));
        }
        String name = request.name() == null || request.name().isBlank() ? null : request.name().trim();
        row.setDestinationId(destinationId);
        row.setName(name);
        row.setMetric(definition.metric());
        row.setLabelFilter(definition.labelFilter());
        row.setOperator(definition.spec().operator());
        row.setThreshold(definition.spec().threshold());
        row.setRecoveryThreshold(definition.spec().recoveryThreshold());
        row.setWindowSeconds(definition.spec().windowSeconds());
        row.setSeverity(request.severity());
        row.setRepeatIntervalSeconds(repeat);
    }

    /** Every server in the rule's group inherits it, so their cached snapshots are stale. */
    private void invalidate(AlertRuleRow row) {
        this.groupRepository.findById(row.getGroupId())
                .ifPresent(group -> this.stateService.invalidateServers(group.getServerIds()));
    }

    private AlertGroupRow requireGroup(UserRow user, long groupId) {
        return this.groupRepository.findByIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new BadRequestException("Choose one of your own alert groups"));
    }

    private AlertRuleRow require(UserRow user, long id) {
        return this.ruleRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Alert rule not found"));
    }

    /** What decides whether existing alert state is still meaningful; notification-only fields are excluded. */
    private static String evaluationFingerprint(AlertRuleRow row) {
        return String.join("|", row.getMetric(), new TreeMap<>(row.getLabelFilter()).toString(),
                row.getOperator().name(), Double.toString(row.getThreshold()), String.valueOf(row.getRecoveryThreshold()),
                Integer.toString(row.getWindowSeconds()));
    }

    private List<AlertRuleResponse> toResponses(List<AlertRuleRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> groupIds = rows.stream().map(AlertRuleRow::getGroupId).collect(Collectors.toSet());
        Map<Long, String> groupNames = new HashMap<>();
        for (AlertGroupRow group : this.groupRepository.findAllById(groupIds)) {
            groupNames.put(group.getId(), group.getName());
        }
        Set<Long> destinationIds = rows.stream().map(AlertRuleRow::getDestinationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> destinationNames = new HashMap<>();
        for (AlertDestinationRow destination : this.destinationRepository.findAllById(destinationIds)) {
            destinationNames.put(destination.getId(), destination.getName());
        }
        Map<Long, AlertRuleEventStat> stats = new HashMap<>();
        for (AlertRuleEventStat stat : this.eventRepository.statsByRuleIds(rows.stream().map(AlertRuleRow::getId).toList())) {
            stats.put(stat.ruleId(), stat);
        }
        return rows.stream().map(row -> AlertRuleResponse.from(row, groupNames.get(row.getGroupId()),
                row.getDestinationId() == null ? null : destinationNames.get(row.getDestinationId()),
                stats.get(row.getId()))).toList();
    }
}
