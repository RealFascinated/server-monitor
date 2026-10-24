package cc.fascinated.monitor.metrics.platform.catalog;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMetricFamilyTest {
    @Test
    void agentFamilies_aggregateServerSeriesAcrossTheFleet() {
        assertEquals(
                List.of("max(monitor_agent_rss_bytes)"),
                PlatformMetricFamily.AGENT_RSS_BYTES_MAX.adminRangeQueries(Duration.ofHours(1))
        );
        assertEquals(
                List.of("sum(monitor_agent_buffered_samples)"),
                PlatformMetricFamily.AGENT_BUFFERED_SAMPLES_TOTAL.adminRangeQueries(Duration.ofHours(1))
        );
    }

    @Test
    void agentCounterFamilies_useRatesPerMinute() {
        assertEquals(
                List.of("sum(rate(monitor_agent_push_failures_total[2m])) * 60"),
                PlatformMetricFamily.AGENT_PUSH_FAILURES_PER_MINUTE.adminRangeQueries(Duration.ofHours(1))
        );
        assertEquals(
                List.of("sum(rate(monitor_agent_buffer_dropped_total[5m])) * 60"),
                PlatformMetricFamily.AGENT_BUFFER_DROPPED_PER_MINUTE.adminRangeQueries(Duration.ofDays(1))
        );
    }

    @Test
    void agentFamilies_liveInTheirOwnSection() {
        List<PlatformMetricFamily> agents = PlatformMetricFamily.forSection(PlatformSection.AGENTS);
        assertFalse(agents.isEmpty());
        assertTrue(agents.stream().noneMatch(PlatformMetricFamily::counter));
        assertEquals("agents", PlatformSection.AGENTS.jsonKey());
    }

    @Test
    void platformFamilies_keepTheirOwnQueries() {
        assertEquals(List.of("monitor_platform_users"), PlatformMetricFamily.USERS.adminRangeQueries(Duration.ofHours(1)));
    }
}
