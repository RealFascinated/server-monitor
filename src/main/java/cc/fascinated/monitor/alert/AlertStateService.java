package cc.fascinated.monitor.alert;

import cc.fascinated.monitor.model.persistance.AlertEventRow;
import cc.fascinated.monitor.model.persistance.AlertGroupRow;
import cc.fascinated.monitor.repository.AlertGroupRepository;
import cc.fascinated.monitor.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * Keeps the in-memory alert state consistent with rule, group and membership changes. In-memory work is deferred
 * until the surrounding transaction commits, so ingest threads cannot reload rows that are about to change.
 */
@Service
public class AlertStateService {
    private final AlertRuleRepository ruleRepository;
    private final AlertGroupRepository groupRepository;
    private final AlertEventService eventService;
    private final AlertRuleCache ruleCache;
    private final AlertEvaluator evaluator;

    public AlertStateService(AlertRuleRepository ruleRepository, AlertGroupRepository groupRepository,
                             AlertEventService eventService, AlertRuleCache ruleCache, AlertEvaluator evaluator) {
        this.ruleRepository = ruleRepository;
        this.groupRepository = groupRepository;
        this.eventService = eventService;
        this.ruleCache = ruleCache;
        this.evaluator = evaluator;
    }

    /** A member who loses access to a server leaves the groups that contained it, and their alerts for it close. */
    @Transactional
    public void onMemberRemoved(long serverId, long userId) {
        List<Long> groupIds = this.groupRepository.findByUserIdAndServerId(userId, serverId).stream()
                .map(AlertGroupRow::getId).toList();
        if (!groupIds.isEmpty()) {
            retireOnServer(this.ruleRepository.findIdsByGroupIds(groupIds), serverId);
            this.groupRepository.removeServerFromUserGroups(userId, serverId);
        }
        invalidateServer(serverId);
    }

    /** A server the user just gained access to joins their auto-add groups. */
    @Transactional
    public void onMemberAdded(long serverId, long userId) {
        for (AlertGroupRow group : this.groupRepository.findByUserIdAndAutoAddServersTrue(userId)) {
            group.getServerIds().add(serverId);
        }
        invalidateServer(serverId);
    }

    /** Closes open events for rules that are changing or going away, and forgets their in-memory state. */
    public void retire(Collection<Long> ruleIds) {
        if (ruleIds.isEmpty()) {
            return;
        }
        this.eventService.closeOpen(ruleIds, AlertEventRow.REASON_RULE_CHANGED);
        afterCommit(() -> this.evaluator.purgeRules(ruleIds));
    }

    /** Same as {@link #retire} but only for one server, e.g. when it leaves a group. */
    public void retireOnServer(Collection<Long> ruleIds, long serverId) {
        if (ruleIds.isEmpty()) {
            return;
        }
        this.eventService.closeOpenForServer(ruleIds, serverId, AlertEventRow.REASON_RULE_CHANGED);
        afterCommit(() -> this.evaluator.purgeServerRules(ruleIds, serverId));
    }

    public void invalidateServer(long serverId) {
        afterCommit(() -> this.ruleCache.invalidate(serverId));
    }

    public void invalidateServers(Collection<Long> serverIds) {
        serverIds.forEach(this::invalidateServer);
    }

    public void invalidateAll() {
        afterCommit(this.ruleCache::invalidateAll);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
