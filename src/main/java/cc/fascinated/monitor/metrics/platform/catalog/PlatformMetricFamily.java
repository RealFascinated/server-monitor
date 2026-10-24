package cc.fascinated.monitor.metrics.platform.catalog;

import cc.fascinated.monitor.metrics.vm.query.PromqlQueryBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
public enum PlatformMetricFamily {
    USERS("monitor_platform_users", PlatformSection.OVERVIEW, false),
    USERS_NEW_24H("monitor_platform_users_new_24h", PlatformSection.OVERVIEW, false),
    DATABASE_SIZE_BYTES("monitor_platform_database_size_bytes", PlatformSection.OVERVIEW, false),
    ACTIVE_SESSIONS("monitor_platform_active_sessions", PlatformSection.OVERVIEW, false),

    SERVERS_TOTAL("monitor_platform_servers_total", PlatformSection.FLEET, false),
    SERVERS_ONLINE("monitor_platform_servers_online", PlatformSection.FLEET, false),
    SERVERS_OFFLINE("monitor_platform_servers_offline", PlatformSection.FLEET, false),
    SERVERS_PENDING("monitor_platform_servers_pending", PlatformSection.FLEET, false),
    SERVERS_NEW_24H("monitor_platform_servers_new_24h", PlatformSection.FLEET, false),
    SERVERS_BY_AGENT_VERSION("monitor_platform_servers_by_agent_version", PlatformSection.FLEET, true, "version"),
    SERVERS_BY_OS("monitor_platform_servers_by_os", PlatformSection.FLEET, true, "os"),

    INGESTS_TOTAL("monitor_platform_ingests_total", PlatformSection.INGEST, false),
    INGEST_AUTH_FAILURES_TOTAL("monitor_platform_ingest_auth_failures_total", PlatformSection.INGEST, false),
    INGEST_BACKFILLED_SAMPLES_TOTAL("monitor_platform_ingest_backfilled_samples_total", PlatformSection.INGEST, false),
    INGEST_REJECTED_SAMPLES_TOTAL("monitor_platform_ingest_rejected_samples_total", PlatformSection.INGEST, true, "reason"),
    INGEST_DURATION_SECONDS("monitor_platform_ingest_duration_seconds", PlatformSection.INGEST, false, true),
    INGEST_PAYLOAD_BYTES("monitor_platform_ingest_payload_bytes", PlatformSection.INGEST, false, true),

    AGENT_RSS_BYTES_MAX("monitor_platform_agent_rss_bytes_max", PlatformSection.AGENTS, false),
    AGENT_RSS_BYTES_AVG("monitor_platform_agent_rss_bytes_avg", PlatformSection.AGENTS, false),
    AGENT_HEAP_IN_USE_BYTES_MAX("monitor_platform_agent_heap_in_use_bytes_max", PlatformSection.AGENTS, false),
    AGENT_GOROUTINES_MAX("monitor_platform_agent_goroutines_max", PlatformSection.AGENTS, false),
    AGENT_OPEN_FDS_MAX("monitor_platform_agent_open_fds_max", PlatformSection.AGENTS, false),
    AGENT_PUSH_DURATION_MS_AVG("monitor_platform_agent_push_duration_ms_avg", PlatformSection.AGENTS, false),
    AGENT_PUSH_DURATION_MS_MAX("monitor_platform_agent_push_duration_ms_max", PlatformSection.AGENTS, false),
    AGENT_PUSH_PAYLOAD_BYTES_AVG("monitor_platform_agent_push_payload_bytes_avg", PlatformSection.AGENTS, false),
    AGENT_PUSH_FAILURES_PER_MINUTE("monitor_platform_agent_push_failures_per_minute", PlatformSection.AGENTS, false),
    AGENT_BUFFERED_SAMPLES_TOTAL("monitor_platform_agent_buffered_samples_total", PlatformSection.AGENTS, false),
    AGENT_BUFFER_OLDEST_AGE_SECONDS_MAX("monitor_platform_agent_buffer_oldest_age_seconds_max", PlatformSection.AGENTS, false),
    AGENT_BUFFER_DROPPED_PER_MINUTE("monitor_platform_agent_buffer_dropped_per_minute", PlatformSection.AGENTS, false),

    VM_STORAGE_SIZE_BYTES("monitor_platform_vm_storage_size_bytes", PlatformSection.VM, false),
    VM_DATAPOINT_COUNT("monitor_platform_vm_datapoint_count", PlatformSection.VM, false),
    VM_QUERIES_TOTAL("monitor_platform_vm_queries_total", PlatformSection.VM, false),
    VM_QUERY_ERRORS_TOTAL("monitor_platform_vm_query_errors_total", PlatformSection.VM, false),
    VM_QUERY_DURATION_SECONDS("monitor_platform_vm_query_duration_seconds", PlatformSection.VM, false, true),
    VM_WRITES_TOTAL("monitor_platform_vm_writes_total", PlatformSection.VM, false),
    VM_WRITE_ERRORS_TOTAL("monitor_platform_vm_write_errors_total", PlatformSection.VM, false),
    VM_WRITE_DURATION_SECONDS("monitor_platform_vm_write_duration_seconds", PlatformSection.VM, false, true),

    JVM_HEAP_USED_BYTES("monitor_platform_jvm_heap_used_bytes", PlatformSection.JVM, false),
    JVM_HEAP_MAX_BYTES("monitor_platform_jvm_heap_max_bytes", PlatformSection.JVM, false),
    JVM_NONHEAP_USED_BYTES("monitor_platform_jvm_nonheap_used_bytes", PlatformSection.JVM, false),
    JVM_THREAD_COUNT("monitor_platform_jvm_thread_count", PlatformSection.JVM, false),
    JVM_PROCESS_CPU_LOAD("monitor_platform_jvm_process_cpu_load", PlatformSection.JVM, false),
    JVM_UPTIME_SECONDS("monitor_platform_jvm_uptime_seconds", PlatformSection.JVM, false),
    JVM_PROCESS_RSS_BYTES("monitor_platform_jvm_process_rss_bytes", PlatformSection.JVM, false),

    HTTP_REQUESTS_TOTAL("monitor_platform_http_requests_total", PlatformSection.HTTP, true, "method", "path", "status"),
    HTTP_REQUEST_DURATION_SECONDS("monitor_platform_http_request_duration_seconds", PlatformSection.HTTP, true, true, "method", "path");

    private final String metricName;
    private final PlatformSection section;
    private final boolean labeled;
    private final boolean histogram;
    private final String[] labelNames;

    PlatformMetricFamily(String metricName, PlatformSection section, boolean labeled, boolean histogram) {
        this(metricName, section, labeled, histogram, new String[0]);
    }

    PlatformMetricFamily(String metricName, PlatformSection section, boolean labeled, String... labelNames) {
        this(metricName, section, labeled, false, labelNames);
    }

    PlatformMetricFamily(String metricName, PlatformSection section, boolean labeled, boolean histogram, String... labelNames) {
        this.metricName = metricName;
        this.section = section;
        this.labeled = labeled;
        this.histogram = histogram;
        this.labelNames = labelNames;
    }

    public List<String> rangeQueries() {
        String fleetQuery = fleetAggregateQuery(null);
        if (fleetQuery != null) {
            return List.of(fleetQuery);
        }
        if (!this.histogram) {
            return List.of(this.metricName);
        }
        return List.of(this.metricName + "_count", this.metricName + "_sum");
    }

    /**
     * PromQL range queries for the admin dashboard. Counters return per-minute rates;
     * histograms return interval averages via {@code rate(sum) / rate(count)}.
     */
    public List<String> adminRangeQueries(Duration span) {
        if (this.histogram && this.labeled) {
            return List.of();
        }
        String rateWindow = rateWindowFor(span);
        String fleetQuery = fleetAggregateQuery(rateWindow);
        if (fleetQuery != null) {
            return List.of(fleetQuery);
        }
        if (this.counter()) {
            return List.of(
                    PromqlQueryBuilder.metric(this.metricName).rate(rateWindow).multiply(60).build()
            );
        }
        if (this.histogram) {
            return List.of(histogramAverageRate(this.metricName, rateWindow));
        }
        return this.rangeQueries();
    }

    public boolean counter() {
        return switch (this) {
            case INGESTS_TOTAL, INGEST_AUTH_FAILURES_TOTAL, INGEST_BACKFILLED_SAMPLES_TOTAL, INGEST_REJECTED_SAMPLES_TOTAL,
                 VM_QUERIES_TOTAL, VM_QUERY_ERRORS_TOTAL, VM_WRITES_TOTAL, VM_WRITE_ERRORS_TOTAL,
                 HTTP_REQUESTS_TOTAL -> true;
            default -> false;
        };
    }

    /**
     * Agent families are not written by the platform recorder: they aggregate the per-server agent
     * self-metrics across the whole fleet at query time. Returns {@code null} for every other family.
     */
    private String fleetAggregateQuery(String rateWindow) {
        String window = rateWindow == null ? "2m" : rateWindow;
        return switch (this) {
            case AGENT_RSS_BYTES_MAX -> "max(monitor_agent_rss_bytes)";
            case AGENT_RSS_BYTES_AVG -> "avg(monitor_agent_rss_bytes)";
            case AGENT_HEAP_IN_USE_BYTES_MAX -> "max(monitor_agent_heap_in_use_bytes)";
            case AGENT_GOROUTINES_MAX -> "max(monitor_agent_goroutines)";
            case AGENT_OPEN_FDS_MAX -> "max(monitor_agent_open_fds)";
            case AGENT_PUSH_DURATION_MS_AVG -> "avg(monitor_agent_last_push_duration_ms)";
            case AGENT_PUSH_DURATION_MS_MAX -> "max(monitor_agent_last_push_duration_ms)";
            case AGENT_PUSH_PAYLOAD_BYTES_AVG -> "avg(monitor_agent_last_push_payload_bytes)";
            case AGENT_PUSH_FAILURES_PER_MINUTE ->
                    "sum(rate(monitor_agent_push_failures_total[" + window + "])) * 60";
            case AGENT_BUFFERED_SAMPLES_TOTAL -> "sum(monitor_agent_buffered_samples)";
            case AGENT_BUFFER_OLDEST_AGE_SECONDS_MAX -> "max(monitor_agent_buffer_oldest_age_seconds)";
            case AGENT_BUFFER_DROPPED_PER_MINUTE ->
                    "sum(rate(monitor_agent_buffer_dropped_total[" + window + "])) * 60";
            default -> null;
        };
    }

    private static String rateWindowFor(Duration span) {
        return span.compareTo(Duration.ofHours(6)) <= 0 ? "2m" : "5m";
    }

    private static String histogramAverageRate(String metricName, String rateWindow) {
        String sumRate = PromqlQueryBuilder.metric(metricName + "_sum").rate(rateWindow).build();
        String countRate = PromqlQueryBuilder.metric(metricName + "_count").rate(rateWindow).build();
        return "(" + sumRate + ") / (" + countRate + ")";
    }

    public static List<PlatformMetricFamily> forSection(PlatformSection section) {
        return Arrays.stream(values()).filter(family -> family.section == section).toList();
    }

    public String rangeQuery() {
        return this.metricName;
    }
}
