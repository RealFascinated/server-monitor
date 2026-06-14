package cc.fascinated.monitor.metrics.platform;

import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.platform.catalog.PlatformSection;
import cc.fascinated.monitor.metrics.vm.assembler.TimeSeriesAssembly;
import cc.fascinated.monitor.metrics.vm.MetricTimeGrid;
import cc.fascinated.monitor.metrics.vm.query.Promql;
import cc.fascinated.monitor.metrics.vm.query.VmTimeSeries;
import cc.fascinated.monitor.model.domain.metric.MetricQueryWindow;
import cc.fascinated.monitor.model.dto.response.metrics.LabeledSeries;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PlatformMetricsAssembler {

    public static Map<String, Object> assembleSections(MetricQueryWindow window,
                                                       Map<PlatformMetricFamily, List<VmTimeSeries>> seriesByFamily) {
        MetricTimeGrid grid = MetricTimeGrid.from(window);
        Map<String, Object> sections = new LinkedHashMap<>();
        for (PlatformSection section : PlatformSection.values()) {
            Object sectionData = assembleSection(section, grid, seriesByFamily);
            if (sectionData != null) {
                sections.put(section.jsonKey(), sectionData);
            }
        }
        return sections;
    }

    private static Object assembleSection(PlatformSection section, MetricTimeGrid grid,
                                          Map<PlatformMetricFamily, List<VmTimeSeries>> seriesByFamily) {
        if (section == PlatformSection.FLEET) {
            return assembleFleetSection(grid, seriesByFamily);
        }
        Map<String, List<Double>> scalars = new LinkedHashMap<>();
        Map<String, LabeledSeries> labeled = new LinkedHashMap<>();
        for (PlatformMetricFamily family : PlatformMetricFamily.forSection(section)) {
            List<VmTimeSeries> series = seriesByFamily.getOrDefault(family, List.of());
            if (family.labeled()) {
                ingestLabeled(grid, labeled, family, series);
            } else if (family.histogram()) {
                ingestHistogramAverage(grid, scalars, family, series);
            } else {
                ingestScalars(grid, scalars, family, series);
            }
        }
        if (!scalars.isEmpty() && !labeled.isEmpty()) {
            Map<String, Object> mixed = new LinkedHashMap<>();
            mixed.putAll(scalars);
            mixed.putAll(labeled);
            return mixed;
        }
        if (!labeled.isEmpty()) {
            return labeled;
        }
        if (!scalars.isEmpty()) {
            return scalars;
        }
        return null;
    }

    private static Object assembleFleetSection(MetricTimeGrid grid,
                                               Map<PlatformMetricFamily, List<VmTimeSeries>> seriesByFamily) {
        Map<String, List<Double>> scalars = new LinkedHashMap<>();
        Map<String, LabeledSeries> byOs = new LinkedHashMap<>();
        Map<String, LabeledSeries> byAgentVersion = new LinkedHashMap<>();

        for (PlatformMetricFamily family : PlatformMetricFamily.forSection(PlatformSection.FLEET)) {
            List<VmTimeSeries> series = seriesByFamily.getOrDefault(family, List.of());
            if (family == PlatformMetricFamily.SERVERS_BY_OS) {
                ingestLabeled(grid, byOs, family, series);
            } else if (family == PlatformMetricFamily.SERVERS_BY_AGENT_VERSION) {
                ingestLabeled(grid, byAgentVersion, family, series);
            } else {
                ingestScalars(grid, scalars, family, series);
            }
        }

        Map<String, Object> fleet = new LinkedHashMap<>();
        Map<String, List<Double>> prunedScalars = TimeSeriesAssembly.pruneScalar(scalars);
        if (prunedScalars != null) {
            fleet.putAll(prunedScalars);
        }
        List<LabeledSeries> byOsEntries = TimeSeriesAssembly.pruneLabeled(byOs);
        if (byOsEntries != null) {
            fleet.put("byOs", byOsEntries);
        }
        List<LabeledSeries> byAgentVersionEntries = TimeSeriesAssembly.pruneLabeled(byAgentVersion);
        if (byAgentVersionEntries != null) {
            fleet.put("byAgentVersion", byAgentVersionEntries);
        }
        return fleet.isEmpty() ? null : fleet;
    }

    private static void ingestScalars(MetricTimeGrid grid, Map<String, List<Double>> scalars,
                                      PlatformMetricFamily family, List<VmTimeSeries> series) {
        if (series.isEmpty()) {
            return;
        }
        scalars.put(fieldName(family), grid.align(series.getFirst()));
    }

    private static void ingestHistogramAverage(MetricTimeGrid grid, Map<String, List<Double>> scalars,
                                             PlatformMetricFamily family, List<VmTimeSeries> series) {
        if (series.isEmpty()) {
            return;
        }
        scalars.put(fieldName(family), grid.align(series.getFirst()));
    }

    private static void ingestLabeled(MetricTimeGrid grid, Map<String, LabeledSeries> labeled,
                                      PlatformMetricFamily family, List<VmTimeSeries> series) {
        String[] labelNames = family.labelNames();
        String fieldName = fieldName(family);
        for (VmTimeSeries entry : series) {
            TimeSeriesAssembly.ingestLabeled(grid, labeled, entry, fieldName, labelNames, labelNames, false);
        }
    }

    private static String fieldName(PlatformMetricFamily family) {
        return Promql.toCamelCase(family.metricName().substring("monitor_platform_".length()));
    }
}
