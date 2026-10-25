package cc.fascinated.monitor.repository;

import java.time.Instant;

public record AlertRuleEventStat(Long ruleId, Long firingCount, Instant lastFiredAt) {}
