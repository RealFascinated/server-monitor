package cc.fascinated.monitor.metrics.vm.query;

import java.util.List;
import java.util.Map;

public record VmTimeSeries(Map<String, String> labels, List<VmSample> samples) {}
