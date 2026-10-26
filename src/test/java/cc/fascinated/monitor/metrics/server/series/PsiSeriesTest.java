package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.PsiMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PsiSeriesTest {
    record Seen(String metric, double value) {}

    /** Verbatim psiMetrics object from {@code monitor-agent print} on a Linux host. */
    private static final String AGENT_PSI_JSON = """
            {
              "cpuSomeAvg10Pct": 1.59,
              "cpuSomeAvg60Pct": 1.93,
              "cpuSomeAvg300Pct": 0.98,
              "cpuSomeTotalMs": 3551388.588,
              "cpuFullAvg10Pct": 0,
              "cpuFullAvg60Pct": 0,
              "cpuFullAvg300Pct": 0,
              "cpuFullTotalMs": 0,
              "memorySomeAvg10Pct": 0,
              "memorySomeAvg60Pct": 0,
              "memorySomeAvg300Pct": 0,
              "memorySomeTotalMs": 600291.737,
              "memoryFullAvg10Pct": 0,
              "memoryFullAvg60Pct": 0,
              "memoryFullAvg300Pct": 0,
              "memoryFullTotalMs": 561968.618,
              "ioSomeAvg10Pct": 0.01,
              "ioSomeAvg60Pct": 0.07,
              "ioSomeAvg300Pct": 0.03,
              "ioSomeTotalMs": 817305.701,
              "ioFullAvg10Pct": 0.01,
              "ioFullAvg60Pct": 0.04,
              "ioFullAvg300Pct": 0.01,
              "ioFullTotalMs": 608073.228
            }
            """;

    @Test
    void writesEveryAgentFieldUnderItsCatalogName() throws Exception {
        PsiMetrics psi = new ObjectMapper().readValue(AGENT_PSI_JSON, PsiMetrics.class);
        List<Seen> seen = write(psi);

        assertThat(seen).hasSize(PsiSeries.values().length);
        assertThat(seen).contains(
                new Seen("monitor_psi_cpu_some_avg10_pct", 1.59),
                new Seen("monitor_psi_cpu_some_total_ms", 3551388.588),
                new Seen("monitor_psi_memory_full_avg300_pct", 0),
                new Seen("monitor_psi_io_full_total_ms", 608073.228)
        );
    }

    @Test
    void absentFieldsAreSkippedRatherThanWrittenAsZero() throws Exception {
        PsiMetrics psi = new ObjectMapper().readValue(
                """
                        {"cpuSomeAvg10Pct": 2.5, "memorySomeTotalMs": 12.5}
                        """,
                PsiMetrics.class);

        assertThat(write(psi)).containsExactly(
                new Seen("monitor_psi_cpu_some_avg10_pct", 2.5),
                new Seen("monitor_psi_memory_some_total_ms", 12.5)
        );
    }

    private static List<Seen> write(PsiMetrics psi) {
        List<Seen> seen = new ArrayList<>();
        MetricWriteContext ctx = new MetricWriteContext(new StringBuilder(), 7, 1_700_000_000L,
                (metric, value, labels) -> seen.add(new Seen(metric, value)));
        PsiSeries.write(ctx, psi);
        return seen;
    }
}