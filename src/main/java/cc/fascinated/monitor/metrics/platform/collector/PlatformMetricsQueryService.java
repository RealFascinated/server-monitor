package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.repository.UserRepository;
import cc.fascinated.monitor.repository.UserSessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformMetricsQueryService {
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final UserSessionRepository userSessionRepository;
    private final JdbcTemplate jdbcTemplate;

    public PlatformMetricsQueryService(UserRepository userRepository, ServerRepository serverRepository,
                                       UserSessionRepository userSessionRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.serverRepository = serverRepository;
        this.userSessionRepository = userSessionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        return this.userRepository.count();
    }

    public long countServers() {
        return this.serverRepository.count();
    }

    public long countNewUsers24h() {
        return queryLong("SELECT COUNT(*) FROM users WHERE created_at > NOW() - INTERVAL '24 hours'");
    }

    public long countNewServers24h() {
        return queryLong("SELECT COUNT(*) FROM servers WHERE created_at > NOW() - INTERVAL '24 hours'");
    }

    public long countActiveSessions() {
        return this.userSessionRepository.countByExpiresAtAfter(Instant.now());
    }

    public ServerStatusCounts serverStatusCounts() {
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList("""
                SELECT status, COUNT(*)::bigint AS cnt
                FROM servers
                GROUP BY status
                """);
        long online = 0;
        long offline = 0;
        long pending = 0;
        for (Map<String, Object> row : rows) {
            Object statusValue = row.get("status");
            if (!(statusValue instanceof Number statusNumber)) {
                continue;
            }
            long count = ((Number) row.get("cnt")).longValue();
            int ordinal = statusNumber.intValue();
            if (ordinal == ServerStatus.ONLINE.ordinal()) {
                online = count;
            } else if (ordinal == ServerStatus.OFFLINE.ordinal()) {
                offline = count;
            } else if (ordinal == ServerStatus.PENDING.ordinal()) {
                pending = count;
            }
        }
        return new ServerStatusCounts(online + offline + pending, online, offline, pending);
    }

    public Map<String, Long> serversByAgentVersion() {
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList("""
                SELECT COALESCE(NULLIF(agent_version, ''), 'unknown') AS label, COUNT(*)::bigint AS cnt
                FROM servers
                GROUP BY agent_version
                """);
        return toLabelMap(rows);
    }

    public Map<String, Long> serversByOs() {
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList("""
                SELECT COALESCE(NULLIF(si.os_name, ''), 'unknown') AS label, COUNT(*)::bigint AS cnt
                FROM servers s
                LEFT JOIN server_inventory si ON si.server_id = s.id
                GROUP BY si.os_name
                """);
        return toLabelMap(rows);
    }

    public long databaseSizeBytes() {
        return queryLong("SELECT pg_database_size(current_database())");
    }

    private long queryLong(String sql, Object... args) {
        Long value = this.jdbcTemplate.queryForObject(sql, Long.class, args);
        return value != null ? value : 0L;
    }

    private static Map<String, Long> toLabelMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("label")), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    public record ServerStatusCounts(long total, long online, long offline, long pending) {}
}
