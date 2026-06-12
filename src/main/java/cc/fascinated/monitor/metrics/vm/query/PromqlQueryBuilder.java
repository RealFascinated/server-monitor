package cc.fascinated.monitor.metrics.vm.query;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromqlQueryBuilder {
    private String expression;

    public static PromqlQueryBuilder metric(String name) {
        return new PromqlQueryBuilder(name);
    }

    public PromqlQueryBuilder rate(String window) {
        this.expression = "rate(" + this.expression + "[" + window + "])";
        return this;
    }

    public PromqlQueryBuilder increase(String window) {
        this.expression = "increase(" + this.expression + "[" + window + "])";
        return this;
    }

    public PromqlQueryBuilder multiply(double factor) {
        this.expression = this.expression + " * " + formatDouble(factor);
        return this;
    }

    public PromqlQueryBuilder sumBy(String... labels) {
        this.expression = "sum(" + this.expression + ") by (" + String.join(", ", labels) + ")";
        return this;
    }

    public PromqlQueryBuilder histogramQuantile(double quantile, String rateWindow) {
        String rateExpr = "rate(" + this.expression + "_bucket[" + rateWindow + "])";
        this.expression = "histogram_quantile(" + formatDouble(quantile) + ", sum(" + rateExpr + ") by (le))";
        return this;
    }

    public String build() {
        return this.expression;
    }

    private static String formatDouble(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
