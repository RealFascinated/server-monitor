package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.model.dto.response.alert.AlertEventResponse;
import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.model.persistance.AlertRuleRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.repository.AlertEventRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.util.Pagination;
import jakarta.persistence.criteria.Subquery;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The alert log: events for the caller's own rules only. */
@Service
public class AlertHistoryService {
    private static final int MAX_PAGE_SIZE = 50;

    private final AlertEventRepository eventRepository;
    private final AlertRuleRepository ruleRepository;
    private final ServerRepository serverRepository;

    public AlertHistoryService(AlertEventRepository eventRepository, AlertRuleRepository ruleRepository,
                               ServerRepository serverRepository) {
        this.eventRepository = eventRepository;
        this.ruleRepository = ruleRepository;
        this.serverRepository = serverRepository;
    }

    @Transactional(readOnly = true)
    public Pagination.Page<AlertEventResponse> list(UserRow user, int page, int count, @Nullable Long ruleId,
                                                    @Nullable Long groupId, @Nullable Long serverId,
                                                    @Nullable String state) {
        if (count < 1 || count > MAX_PAGE_SIZE) {
            throw new BadRequestException("count must be between 1 and %d".formatted(MAX_PAGE_SIZE));
        }
        if (state != null && !state.equals("firing") && !state.equals("resolved")) {
            throw new BadRequestException("state must be firing or resolved");
        }
        Specification<AlertEventRow> spec = ownedBy(user.getId());
        if (ruleId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ruleId"), ruleId));
        }
        if (groupId != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> rules = query.subquery(Long.class);
                var rule = rules.from(AlertRuleRow.class);
                rules.select(rule.get("id")).where(cb.equal(rule.get("groupId"), groupId));
                return root.get("ruleId").in(rules);
            });
        }
        if (serverId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("serverId"), serverId));
        }
        if ("firing".equals(state)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("resolvedAt")));
        } else if ("resolved".equals(state)) {
            spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("resolvedAt")));
        }
        Specification<AlertEventRow> finalSpec = spec;
        return new Pagination<AlertEventResponse>()
                .setItemsPerPage(count)
                .setTotalItems(this.eventRepository.count(finalSpec))
                .getPage(page, callback -> toResponses(this.eventRepository.findAll(finalSpec,
                        PageRequest.of(callback.skip() / callback.limit(), callback.limit(),
                                Sort.by(Sort.Direction.DESC, "startedAt", "id"))).getContent()));
    }

    /** How many of the caller's alerts are firing right now, for the sidebar badge. */
    @Transactional(readOnly = true)
    public long firingCount(UserRow user) {
        return this.eventRepository.count(ownedBy(user.getId()).and((root, query, cb) -> cb.isNull(root.get("resolvedAt"))));
    }

    private static Specification<AlertEventRow> ownedBy(long userId) {
        return (root, query, cb) -> {
            Subquery<Long> rules = query.subquery(Long.class);
            var rule = rules.from(AlertRuleRow.class);
            rules.select(rule.get("id")).where(cb.equal(rule.get("userId"), userId));
            return root.get("ruleId").in(rules);
        };
    }

    private List<AlertEventResponse> toResponses(List<AlertEventRow> events) {
        Set<Long> ruleIds = events.stream().map(AlertEventRow::getRuleId).collect(Collectors.toSet());
        Map<Long, AlertRuleRow> rules = new HashMap<>();
        for (AlertRuleRow rule : this.ruleRepository.findAllById(ruleIds)) {
            rules.put(rule.getId(), rule);
        }
        Set<Long> serverIds = events.stream().map(AlertEventRow::getServerId).collect(Collectors.toSet());
        Map<Long, String> serverNames = new HashMap<>();
        for (ServerRow server : this.serverRepository.findAllById(serverIds)) {
            serverNames.put(server.getId(), server.getServerName());
        }
        return events.stream().map(event -> AlertEventResponse.from(event, rules.get(event.getRuleId()),
                serverNames.get(event.getServerId()))).toList();
    }
}
