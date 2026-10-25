package cc.fascinated.monitor.metrics.vm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricWriteContextTest {
    record Seen(String metric, double value, Map<String, String> labels) {}

    @Test
    void sinkSeesTheMergedLabelsTheWriterUses() {
        List<Seen> seen = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        MetricWriteContext ctx = new MetricWriteContext(buffer, 7, 1_700_000_000L,
                (metric, value, labels) -> seen.add(new Seen(metric, value, labels)));

        ctx.withLabel("disk", "/").gauge("monitor_disk_usage_percent", 42.5);

        assertThat(seen).containsExactly(new Seen("monitor_disk_usage_percent", 42.5, Map.of("server_id", "7", "disk", "/")));
        assertThat(buffer.toString()).contains("monitor_disk_usage_percent").contains("disk=\"/\"");
    }

    @Test
    void withoutASinkTheOutputIsIdentical() {
        StringBuilder plain = new StringBuilder();
        StringBuilder observed = new StringBuilder();
        new MetricWriteContext(plain, 7, 1_700_000_000L).withLabel("disk", "/").gauge("m", 1);
        new MetricWriteContext(observed, 7, 1_700_000_000L, (metric, value, labels) -> { })
                .withLabel("disk", "/").gauge("m", 1);

        assertThat(observed.toString()).isEqualTo(plain.toString());
    }
}
