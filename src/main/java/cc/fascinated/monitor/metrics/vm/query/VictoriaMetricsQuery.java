package cc.fascinated.monitor.metrics.vm.query;

import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VictoriaMetricsQuery {
    private final String promql;
    private final Instant from;
    private final Instant to;
    private final Duration step;
    private final Instant at;

    private VictoriaMetricsQuery(String promql, Instant from, Instant to, Duration step, Instant at) {
        this.promql = promql;
        this.from = from;
        this.to = to;
        this.step = step;
        this.at = at;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String promql() {
        return this.promql;
    }

    public boolean isRange() {
        return this.from != null && this.to != null;
    }

    public Instant from() {
        return this.from;
    }

    public Instant to() {
        return this.to;
    }

    public Duration step() {
        return this.step;
    }

    public Instant at() {
        return this.at;
    }

    public String stepParam() {
        return formatStep(this.step);
    }

    static String formatStep(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds >= 3600 && seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds >= 60 && seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    public static final class Builder {
        private String rawQuery;
        private String metricName;
        private final Map<String, String> labels = new LinkedHashMap<>();
        private Instant from;
        private Instant to;
        private Duration step;
        private Instant at;
        private int autoStepMaxPoints;

        public Builder query(String promql) {
            this.rawQuery = promql;
            this.metricName = null;
            this.labels.clear();
            return this;
        }

        public Builder metric(VmGaugeSeries series) {
            return metric(series.metricName());
        }

        public Builder metric(String metricName) {
            this.rawQuery = null;
            this.metricName = metricName;
            return this;
        }

        public Builder serverId(long serverId) {
            return label("server_id", Long.toString(serverId));
        }

        public Builder label(String key, String value) {
            this.labels.put(key, value);
            return this;
        }

        public Builder from(Instant from) {
            this.from = from;
            return this;
        }

        public Builder to(Instant to) {
            this.to = to;
            return this;
        }

        public Builder step(Duration step) {
            this.step = step;
            return this;
        }

        public Builder at(Instant at) {
            this.at = at;
            return this;
        }

        public Builder last(Duration duration) {
            Instant now = Instant.now();
            this.to = now;
            this.from = now.minus(duration);
            return this;
        }

        public Builder autoStep(int maxPoints) {
            this.autoStepMaxPoints = maxPoints;
            return this;
        }

        public VictoriaMetricsQuery build() {
            String promql = resolvePromql();
            if (promql == null || promql.isBlank()) {
                throw new IllegalArgumentException("Query requires promql via query() or metric()");
            }

            boolean hasFrom = this.from != null;
            boolean hasTo = this.to != null;
            if (hasFrom != hasTo) {
                throw new IllegalArgumentException("Range queries require both from and to");
            }

            if (hasFrom) {
                Duration resolvedStep = resolveStep();
                if (resolvedStep == null || resolvedStep.isZero() || resolvedStep.isNegative()) {
                    throw new IllegalArgumentException("Range queries require a positive step");
                }
                if (!this.from.isBefore(this.to)) {
                    throw new IllegalArgumentException("from must be before to");
                }
                return new VictoriaMetricsQuery(promql, this.from, this.to, resolvedStep, null);
            }

            Instant resolvedAt = this.at != null ? this.at : Instant.now();
            return new VictoriaMetricsQuery(promql, null, null, null, resolvedAt);
        }

        private String resolvePromql() {
            if (this.rawQuery != null) {
                return this.rawQuery;
            }
            if (this.metricName == null) {
                return null;
            }
            if (this.labels.isEmpty()) {
                return this.metricName;
            }
            StringBuilder builder = new StringBuilder(this.metricName);
            builder.append('{');
            boolean first = true;
            for (Map.Entry<String, String> entry : this.labels.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(entry.getKey());
                builder.append("=\"");
                appendEscapedLabelValue(builder, entry.getValue());
                builder.append('"');
            }
            builder.append('}');
            return builder.toString();
        }

        private Duration resolveStep() {
            if (this.step != null) {
                return this.step;
            }
            if (this.autoStepMaxPoints > 0 && this.from != null && this.to != null) {
                long rangeSeconds = Duration.between(this.from, this.to).getSeconds();
                long stepSeconds = Math.max(1, rangeSeconds / this.autoStepMaxPoints);
                return Duration.ofSeconds(stepSeconds);
            }
            return null;
        }

        private static void appendEscapedLabelValue(StringBuilder builder, String value) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\' || c == '"') {
                    builder.append('\\');
                }
                builder.append(c);
            }
        }
    }
}
