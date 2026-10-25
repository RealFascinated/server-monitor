package cc.fascinated.monitor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberUtilsTest {
    @Test
    void formatDropsTrailingZeros() {
        assertThat(NumberUtils.format(90, 2)).isEqualTo("90");
        assertThat(NumberUtils.format(96.55, 2)).isEqualTo("96.55");
        assertThat(NumberUtils.format(42.10, 2)).isEqualTo("42.1");
        assertThat(NumberUtils.format(-0.001, 2)).isEqualTo("0");
        assertThat(NumberUtils.format(Double.NaN, 2)).isEqualTo("NaN");
    }

    @Test
    void formatBytesUsesBinaryPrefixes() {
        assertThat(NumberUtils.formatBytes(512)).isEqualTo("512 B");
        assertThat(NumberUtils.formatBytes(1536)).isEqualTo("1.5 KB");
        assertThat(NumberUtils.formatBytes(5L * 1024 * 1024 * 1024)).isEqualTo("5 GB");
    }
}
