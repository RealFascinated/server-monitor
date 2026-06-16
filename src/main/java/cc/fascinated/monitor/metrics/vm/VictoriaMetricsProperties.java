package cc.fascinated.monitor.metrics.vm;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "monitor.victoriametrics")
public class VictoriaMetricsProperties {
    @NotBlank
    private String importUrl = "http://localhost:8428/api/v1/import/prometheus";

    private String deleteSeriesUrl = "";

    private String deleteToken = "";

    public String getBaseUrl() {
        if (this.importUrl.endsWith("/api/v1/import/prometheus")) {
            return this.importUrl.substring(0, this.importUrl.length() - "/api/v1/import/prometheus".length());
        }
        if (this.importUrl.endsWith("/import/prometheus")) {
            return this.importUrl.substring(0, this.importUrl.length() - "/import/prometheus".length());
        }
        int apiIndex = this.importUrl.indexOf("/api/");
        if (apiIndex > 0) {
            return this.importUrl.substring(0, apiIndex);
        }
        int lastSlash = this.importUrl.lastIndexOf('/');
        return lastSlash > 0 ? this.importUrl.substring(0, lastSlash) : this.importUrl;
    }

    public String getQueryUrl() {
        return getBaseUrl() + "/api/v1/query";
    }

    public String getQueryRangeUrl() {
        return getBaseUrl() + "/api/v1/query_range";
    }

    public String getDeleteSeriesUrl() {
        if (this.deleteSeriesUrl != null && !this.deleteSeriesUrl.isBlank()) {
            return this.deleteSeriesUrl;
        }
        return getBaseUrl() + "/api/v1/admin/tsdb/delete_series";
    }

    public String getMetricsUrl() {
        return getBaseUrl() + "/metrics";
    }
}
