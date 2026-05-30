package cc.fascinated.monitor.service;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.repository.ServerRepository;
import cc.fascinated.monitor.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformMetricsQueryService {
    private static final String[] METRIC_TABLES = {
            "server_metrics",
            "server_disk_metrics",
            "server_network_metrics",
            "server_zfs_arc_metrics",
            "server_zfs_pool_metrics",
            "server_docker_container_metrics"
    };

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MonitorMetricsProperties metricsProperties;

    public PlatformMetricsQueryService(UserRepository userRepository, ServerRepository serverRepository,
                                       JdbcTemplate jdbcTemplate, MonitorMetricsProperties metricsProperties) {
        this.userRepository = userRepository;
        this.serverRepository = serverRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.metricsProperties = metricsProperties;
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

    public FleetCounts fleetCounts() {
        long thresholdSeconds = this.metricsProperties.getReportingThreshold().getSeconds();
        Map<String, Object> row = this.jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(*)::bigint AS total,
                    COUNT(*) FILTER (WHERE last_updated > NOW() - (? || ' seconds')::interval)::bigint AS reporting,
                    COUNT(*) FILTER (WHERE last_updated IS NOT NULL AND last_updated <= NOW() - (? || ' seconds')::interval)::bigint AS stale,
                    COUNT(*) FILTER (WHERE last_updated IS NULL)::bigint AS never_reported
                FROM servers
                """, thresholdSeconds, thresholdSeconds);
        return new FleetCounts(
                ((Number) row.get("total")).longValue(),
                ((Number) row.get("reporting")).longValue(),
                ((Number) row.get("stale")).longValue(),
                ((Number) row.get("never_reported")).longValue()
        );
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

    public long tableRowCount(String table) {
        return queryLong("SELECT COUNT(*)::bigint FROM " + table);
    }

    public long tableSizeBytes(String table) {
        return queryLong("SELECT pg_total_relation_size(?::regclass)", table);
    }

    public String[] metricTables() {
        return METRIC_TABLES;
    }

    public long dockerEnabledServers() {
        return countServersWithRecentMetrics("server_docker_container_metrics");
    }

    public long zfsEnabledServers() {
        return queryLong("""
                SELECT COUNT(*)::bigint FROM (
                    SELECT DISTINCT server_id FROM server_zfs_arc_metrics WHERE timestamp > NOW() - (? || ' seconds')::interval
                    UNION
                    SELECT DISTINCT server_id FROM server_zfs_pool_metrics WHERE timestamp > NOW() - (? || ' seconds')::interval
                ) AS enabled
                """, reportingThresholdSeconds(), reportingThresholdSeconds());
    }

    private long countServersWithRecentMetrics(String table) {
        return queryLong(
                "SELECT COUNT(DISTINCT server_id)::bigint FROM " + table + " WHERE timestamp > NOW() - (? || ' seconds')::interval",
                reportingThresholdSeconds()
        );
    }

    private long reportingThresholdSeconds() {
        return this.metricsProperties.getReportingThreshold().getSeconds();
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

    public record FleetCounts(long total, long reporting, long stale, long neverReported) {
        public long offline() {
            return this.total - this.reporting;
        }
    }
}
