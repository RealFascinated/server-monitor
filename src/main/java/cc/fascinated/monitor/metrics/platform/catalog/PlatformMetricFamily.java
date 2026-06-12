package cc.fascinated.monitor.metrics.platform.catalog;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
public enum PlatformMetricFamily {
    USERS("monitor_platform_users", PlatformSection.FLEET, false),
    USERS_NEW_24H("monitor_platform_users_new_24h", PlatformSection.FLEET, false),
    SERVERS_TOTAL("monitor_platform_servers_total", PlatformSection.FLEET, false),
    SERVERS_ONLINE("monitor_platform_servers_online", PlatformSection.FLEET, false),
    SERVERS_OFFLINE("monitor_platform_servers_offline", PlatformSection.FLEET, false),
    SERVERS_PENDING("monitor_platform_servers_pending", PlatformSection.FLEET, false),
    SERVERS_NEW_24H("monitor_platform_servers_new_24h", PlatformSection.FLEET, false),
    SERVERS_BY_AGENT_VERSION("monitor_platform_servers_by_agent_version", PlatformSection.FLEET, true, "version"),
    SERVERS_BY_OS("monitor_platform_servers_by_os", PlatformSection.FLEET, true, "os"),
    DATABASE_SIZE_BYTES("monitor_platform_database_size_bytes", PlatformSection.FLEET, false),
    ACTIVE_SESSIONS("monitor_platform_active_sessions", PlatformSection.FLEET, false),
    WEBSOCKET_CONNECTIONS("monitor_platform_websocket_connections", PlatformSection.FLEET, false),

    INGESTS_TOTAL("monitor_platform_ingests_total", PlatformSection.INGEST, false),
    INGEST_AUTH_FAILURES_TOTAL("monitor_platform_ingest_auth_failures_total", PlatformSection.INGEST, false),
    INGEST_DURATION_SECONDS("monitor_platform_ingest_duration_seconds", PlatformSection.INGEST, false, true),
    INGEST_PAYLOAD_BYTES("monitor_platform_ingest_payload_bytes", PlatformSection.INGEST, false, true),

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
        if (!this.histogram) {
            return List.of(this.metricName);
        }
        return List.of(this.metricName + "_count", this.metricName + "_sum");
    }

    public static List<PlatformMetricFamily> forSection(PlatformSection section) {
        return Arrays.stream(values()).filter(family -> family.section == section).toList();
    }

    public String rangeQuery() {
        return this.metricName;
    }
}
