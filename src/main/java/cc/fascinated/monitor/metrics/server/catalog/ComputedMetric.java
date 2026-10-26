package cc.fascinated.monitor.metrics.server.catalog;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.LongFunction;

@Getter
@Accessors(fluent = true)
public enum ComputedMetric {
    DISK_ETA_UNTIL_FULL(MetricFamily.DISK, "etaUntilFull", ComputedMetric::diskEtaUntilFullPromql),
    SMART_ETA_UNTIL_WORN_OUT(MetricFamily.SMART, "etaUntilWornOut", ComputedMetric::smartEtaUntilWornOutPromql);

    private static final String ETA_GROWTH_WINDOW = "1d";
    private static final long ETA_MIN_HISTORY_SECONDS = 3600;
    private static final long ETA_MAX_SECONDS = 10L * 365 * 86400;
    private static final long WEAR_ETA_MAX_SECONDS = 50L * 365 * 86400;

    private final MetricFamily family;
    private final String fieldName;
    private final LongFunction<String> promqlBuilder;

    ComputedMetric(MetricFamily family, String fieldName, LongFunction<String> promqlBuilder) {
        this.family = family;
        this.fieldName = fieldName;
        this.promqlBuilder = promqlBuilder;
    }

    public String promql(long serverId) {
        return this.promqlBuilder.apply(serverId);
    }

    /**
     * Free space divided by the growth rate, where the rate is a least-squares slope over the trailing
     * {@link #ETA_GROWTH_WINDOW} rather than the difference between two samples, so a single spike or
     * cleanup at either end of the window can't skew it. No point is produced (the chart shows a gap)
     * for disks that are flat or shrinking, have under {@link #ETA_MIN_HISTORY_SECONDS} of history, or
     * would take longer than {@link #ETA_MAX_SECONDS} to fill, since those values are noise, not a forecast.
     */
    private static String diskEtaUntilFullPromql(long serverId) {
        String selector = "{server_id=\"" + serverId + "\"}";
        String total = "monitor_disk_total_bytes" + selector;
        String used = "monitor_disk_used_bytes" + selector;
        String growthPerSecond = "deriv(" + used + "[" + ETA_GROWTH_WINDOW + "])";
        String history = "lifetime(" + used + "[" + ETA_GROWTH_WINDOW + "])";
        return "((" + total + " - " + used + ") / (" + growthPerSecond + " > 0)) < " + ETA_MAX_SECONDS
                + " and (" + history + " >= " + ETA_MIN_HISTORY_SECONDS + ")";
    }

    /**
     * Time until a drive's rated endurance is used up, assuming it keeps wearing at its lifetime average:
     * hours remaining = power-on hours * (100 - used) / used. SMART wear is a whole percent that can sit
     * unchanged for months, so a trend over a recent window would read as zero growth for most drives;
     * the lifetime average needs no history and is available from the first sample. NVMe drives report
     * {@code percentage_used}, SATA SSDs {@code endurance_used_percent}, and the former wins when a drive
     * has both. Drives with no wear yet, already at or past 100%, or further than
     * {@link #WEAR_ETA_MAX_SECONDS} out (where whole-percent rounding makes the figure meaningless) get no point.
     */
    private static String smartEtaUntilWornOutPromql(long serverId) {
        String selector = "{server_id=\"" + serverId + "\"}";
        String used = "((monitor_smart_percentage_used" + selector + " or monitor_smart_endurance_used_percent" + selector
                + ") > 0) < 100";
        String powerOnHours = "monitor_smart_power_on_hours" + selector;
        return "((100 - (" + used + ")) / (" + used + ") * " + powerOnHours + " * 3600) < " + WEAR_ETA_MAX_SECONDS;
    }
}
