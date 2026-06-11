package cc.fascinated.monitor.metrics.vm.query;

import java.time.Instant;

public record VmSample(Instant timestamp, double value) {}
