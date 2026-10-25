package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.config.MonitorAlertProperties;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.exception.impl.ConflictException;
import cc.fascinated.monitor.exception.impl.NotFoundException;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.dto.request.alert.AlertGroupRequest;
import cc.fascinated.monitor.model.dto.request.alert.AlertGroupUpdateRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertGroupResponse;
import cc.fascinated.monitor.model.persistance.AlertDestinationRow;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.AlertDestinationRepository;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.repository.AlertRuleEventStat;
import cc.fascinated.monitor.repository.AlertEventRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.server.ServerService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Alert groups: a set of servers that all inherit the rules attached to the group. */
@Service
public class AlertGroupService {
    private final AlertGroupRepository groupRepository;
    private final AlertRuleRepository ruleRepository;
    private final AlertEventRepository eventRepository;
    private final ServerRepository serverRepository;
    private final ServerService serverService;
    private final AlertStateService stateService;
    private final MonitorAlertProperties properties;
    private final DefaultAlertRuleService defaultRuleService;
    private final AlertDestinationRepository destinationRepository;

    public AlertGroupService(AlertGroupRepository groupRepository, AlertRuleRepository ruleRepository,
                             AlertEventRepository eventRepository, ServerRepository serverRepository,
                             ServerService serverService, AlertStateService stateService,
                             MonitorAlertProperties properties, DefaultAlertRuleService defaultRuleService,
                             AlertDestinationRepository destinationRepository) {
        this.defaultRuleService = defaultRuleService;
        this.destinationRepository = destinationRepository;
        this.groupRepository = groupRepository;
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.serverRepository = serverRepository;
        this.serverService = serverService;
        this.stateService = stateService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<AlertGroupResponse> list(UserRow user) {
        return toResponses(this.groupRepository.findByUserIdOrderByIdAsc(user.getId()));
    }

    @Transactional(readOnly = true)
    public AlertGroupResponse get(UserRow user, long id) {
        return toResponses(List.of(require(user, id))).getFirst();
    }

    @Transactional
    public AlertGroupResponse create(UserRow user, AlertGroupRequest request) {
        if (this.groupRepository.countByUserId(user.getId()) >= this.properties.getMaxGroupsPerUser()) {
            throw new ConflictException("You can have at most %d alert groups".formatted(this.properties.getMaxGroupsPerUser()));
        }
        boolean addDefaults = Boolean.TRUE.equals(request.addDefaultRules());
        if (addDefaults && this.ruleRepository.countByUserId(user.getId()) + this.defaultRuleService.defaultRuleCount()
                > this.properties.getMaxRulesPerUser()) {
            throw new ConflictException("Adding the default alerts would exceed the limit of %d alert rules"
                    .formatted(this.properties.getMaxRulesPerUser()));
        }
        AlertGroupRow row = new AlertGroupRow(user.getId(), request.name().trim(),
                Boolean.TRUE.equals(request.autoAddServers()), Instant.now());
        row.getServerIds().addAll(requireServers(user, request.serverIds()));
        row.setDestinationId(requireDestination(user, request.destinationId()));
        AlertGroupRow saved = this.groupRepository.save(row);
        if (addDefaults) {
            this.defaultRuleService.addTo(user.getId(), saved.getId());
        }
        this.stateService.invalidateServers(saved.getServerIds());
        return toResponses(List.of(saved)).getFirst();
    }

    @Transactional
    public AlertGroupResponse update(UserRow user, long id, AlertGroupUpdateRequest request) {
        AlertGroupRow row = require(user, id);
        if (request.name() != null) {
            row.setName(request.name().trim());
        }
        if (request.autoAddServers() != null) {
            row.setAutoAddServers(request.autoAddServers());
        }
        if (request.serverIds() != null) {
            replaceServers(user, row, requireServers(user, request.serverIds()));
        }
        if (request.destinationId() != null || Boolean.TRUE.equals(request.clearDestination())) {
            row.setDestinationId(requireDestination(user, request.destinationId()));
            this.stateService.invalidateServers(row.getServerIds());
        }
        if (request.enabled() != null && request.enabled() != row.isEnabled()) {
            setEnabled(row, request.enabled());
        }
        return toResponses(List.of(row)).getFirst();
    }

    @Transactional
    public void delete(UserRow user, long id) {
        AlertGroupRow row = require(user, id);
        if (row.isDefaultGroup()) {
            throw new ConflictException("The default alert group cannot be deleted");
        }
        this.stateService.retire(this.ruleRepository.findIdsByGroupIds(List.of(id)));
        Set<Long> affected = new HashSet<>(row.getServerIds());
        this.groupRepository.delete(row);
        this.stateService.invalidateServers(affected);
    }

    AlertGroupRow require(UserRow user, long id) {
        return this.groupRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Alert group not found"));
    }

    /** Turning a group off closes what it has open, since nothing evaluates its rules any more. */
    private void setEnabled(AlertGroupRow row, boolean enabled) {
        row.setEnabled(enabled);
        if (!enabled) {
            this.stateService.retire(this.ruleRepository.findIdsByGroupIds(List.of(row.getId())));
        }
        this.stateService.invalidateServers(row.getServerIds());
    }

    private void replaceServers(UserRow user, AlertGroupRow row, Set<Long> next) {
        Set<Long> removed = new HashSet<>(row.getServerIds());
        removed.removeAll(next);
        Set<Long> added = new HashSet<>(next);
        added.removeAll(row.getServerIds());
        if (!removed.isEmpty()) {
            List<Long> ruleIds = this.ruleRepository.findIdsByGroupIds(List.of(row.getId()));
            removed.forEach(serverId -> this.stateService.retireOnServer(ruleIds, serverId));
        }
        row.getServerIds().removeAll(removed);
        row.getServerIds().addAll(added);
        this.stateService.invalidateServers(removed);
        this.stateService.invalidateServers(added);
    }

    @Nullable
    private Long requireDestination(UserRow user, @Nullable Long destinationId) {
        if (destinationId == null) {
            return null;
        }
        return this.destinationRepository.findByIdAndUserId(destinationId, user.getId())
                .orElseThrow(() -> new BadRequestException("Choose one of your own alert destinations")).getId();
    }

    /** Every server must be one the caller can view metrics for. */
    private Set<Long> requireServers(UserRow user, @Nullable Collection<Long> serverIds) {
        Set<Long> result = new HashSet<>();
        if (serverIds != null) {
            for (Long serverId : serverIds) {
                result.add(this.serverService.requireServer(user, serverId, ServerPermission.VIEW_METRICS).getId());
            }
        }
        return result;
    }

    private List<AlertGroupResponse> toResponses(List<AlertGroupRow> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        Set<Long> serverIds = groups.stream().flatMap(group -> group.getServerIds().stream()).collect(Collectors.toSet());
        Map<Long, String> serverNames = new HashMap<>();
        for (ServerRow server : this.serverRepository.findAllById(serverIds)) {
            serverNames.put(server.getId(), server.getServerName());
        }
        Map<Long, String> destinationNames = new HashMap<>();
        for (AlertDestinationRow destination : this.destinationRepository.findAllById(groups.stream()
                .map(AlertGroupRow::getDestinationId).filter(Objects::nonNull).collect(Collectors.toSet()))) {
            destinationNames.put(destination.getId(), destination.getName());
        }
        List<AlertRuleRow> rules = groups.stream()
                .flatMap(group -> this.ruleRepository.findByGroupId(group.getId()).stream()).toList();
        Map<Long, Long> firingByRule = new HashMap<>();
        if (!rules.isEmpty()) {
            for (AlertRuleEventStat stat : this.eventRepository.statsByRuleIds(rules.stream().map(AlertRuleRow::getId).toList())) {
                firingByRule.put(stat.ruleId(), stat.firingCount());
            }
        }
        Map<Long, List<AlertRuleRow>> rulesByGroup = rules.stream().collect(Collectors.groupingBy(AlertRuleRow::getGroupId));
        return groups.stream().map(group -> {
            List<AlertRuleRow> groupRules = rulesByGroup.getOrDefault(group.getId(), List.of());
            long firing = groupRules.stream().mapToLong(rule -> firingByRule.getOrDefault(rule.getId(), 0L)).sum();
            List<AlertGroupResponse.GroupServer> servers = group.getServerIds().stream()
                    .filter(serverNames::containsKey)
                    .map(id -> new AlertGroupResponse.GroupServer(id, serverNames.get(id)))
                    .sorted(Comparator.comparing(AlertGroupResponse.GroupServer::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return AlertGroupResponse.from(group, destinationNames.get(group.getDestinationId()), servers,
                    groupRules.size(), firing);
        }).toList();
    }
}
