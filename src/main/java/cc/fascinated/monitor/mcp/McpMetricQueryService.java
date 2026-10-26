package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.alert.SeriesKeys;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.mcp.dto.McpMetricQueryResult;
import cc.fascinated.monitor.mcp.dto.McpMetricSeries;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricCatalog;
import cc.fascinated.monitor.metrics.server.series.VmGaugeSeries;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQueryClient;
import cc.fascinated.monitor.metrics.vm.query.VmSample;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.server.ServerPermission;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Range queries for MCP clients. Output is shaped for a model's context: a bounded number of points and series, with
 * summary statistics so a model rarely needs to read the points at all.
 */
@Service
public class McpMetricQueryService {
    private static final Duration DEFAULT_RANGE = Duration.ofHours(1);
    private static final Duration MAX_RANGE = Duration.ofDays(31);
    private static final int MAX_POINTS = 200;
    private static final int MIN_STEP_SECONDS = 15;
    private static final int MAX_SERIES = 20;

    private final ServerService serverService;
    private final VictoriaMetricsQueryClient queryClient;

    public McpMetricQueryService(ServerService serverService, VictoriaMetricsQueryClient queryClient) {
        this.serverService = serverService;
        this.queryClient = queryClient;
    }

    /**
     * @param from        ISO-8601 instant, default one hour before {@code to}
     * @param to          ISO-8601 instant, default now
     * @param stepSeconds requested resolution; raised when it would return more than {@value #MAX_POINTS} points
     * @param includePoints whether to return the samples themselves; the statistics are always returned
     */
    public McpMetricQueryResult query(UserRow user, long serverId, String metric,
                                      @Nullable Map<String, String> labels, @Nullable String from, @Nullable String to,
                                      @Nullable Integer stepSeconds, boolean includePoints) {
        this.serverService.requireServer(user, serverId, ServerPermission.VIEW_METRICS);
        if (!McpMetricCatalogService.isQueryable(metric)) {
            throw new BadRequestException("Unknown metric \"%s\". Use list_metrics to see valid metric ids".formatted(metric));
        }
        List<String> allowedKeys = McpMetricCatalogService.labelKeysOf(metric);
        if (labels != null) {
            for (String key : labels.keySet()) {
                if (!allowedKeys.contains(key)) {
                    throw new BadRequestException("\"%s\" is not a label of %s. %s".formatted(key, metric,
                            allowedKeys.isEmpty() ? "It has no labels" : "Valid labels: " + String.join(", ", allowedKeys)));
                }
            }
        }
        Instant end = parse("to", to, Instant.now());
        Instant start = parse("from", from, end.minus(DEFAULT_RANGE));
        if (!start.isBefore(end)) {
            throw new BadRequestException("from must be before to");
        }
        if (Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw new BadRequestException("The range can be at most %d days".formatted(MAX_RANGE.toDays()));
        }
        long rangeSeconds = Duration.between(start, end).getSeconds();
        int step = (int) Math.max(Math.max(MIN_STEP_SECONDS, stepSeconds == null ? 0 : stepSeconds),
                (rangeSeconds + MAX_POINTS - 1) / MAX_POINTS);
        // Anchor the grid on `end` so the last point is the newest sample rather than up to a step stale.
        start = end.minusSeconds(Math.max(1, rangeSeconds / step) * step);

        VictoriaMetricsQuery.Builder query = VictoriaMetricsQuery.builder()
                .metric(metric).serverId(serverId)
                .from(start).to(end).step(Duration.ofSeconds(step));
        if (labels != null) {
            labels.forEach(query::label);
        }
        List<VmTimeSeries> all = new ArrayList<>(this.queryClient.execute(query.build()).timeSeries());
        all.sort(Comparator.comparing(series -> SeriesKeys.keyOf(SeriesKeys.seriesLabels(series.labels()))));

        List<McpMetricSeries> result = new ArrayList<>();
        for (VmTimeSeries series : all.subList(0, Math.min(MAX_SERIES, all.size()))) {
            if (!series.samples().isEmpty()) {
                result.add(summarise(series, includePoints));
            }
        }
        VmGaugeSeries alertable = VmMetricCatalog.alertable().get(metric);
        AlertInfo info = alertable == null ? null : alertable.alertInfo();
        return new McpMetricQueryResult(metric, info == null ? null : info.label(), info == null ? null : info.unit(),
                start.getEpochSecond(), end.getEpochSecond(), step, result, all.size() > MAX_SERIES);
    }

    private static McpMetricSeries summarise(VmTimeSeries series, boolean includePoints) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        long minAt = 0;
        long maxAt = 0;
        double sum = 0;
        List<Object[]> points = includePoints ? new ArrayList<>(series.samples().size()) : null;
        for (VmSample sample : series.samples()) {
            double value = sample.value();
            long at = sample.timestamp().getEpochSecond();
            if (value < min) {
                min = value;
                minAt = at;
            }
            if (value > max) {
                max = value;
                maxAt = at;
            }
            sum += value;
            if (points != null) {
                points.add(new Object[]{at, round(value)});
            }
        }
        double last = series.samples().getLast().value();
        // __name__ repeats the top-level metric field.
        Map<String, String> labels = new TreeMap<>(SeriesKeys.seriesLabels(series.labels()));
        labels.remove("__name__");
        return new McpMetricSeries(labels, round(min), round(max), round(sum / series.samples().size()),
                round(last), maxAt, minAt, points);
    }

    private static Instant parse(String field, @Nullable String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeException ex) {
            throw new BadRequestException("%s must be an ISO-8601 instant such as 2026-01-31T14:00:00Z".formatted(field));
        }
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }
}
