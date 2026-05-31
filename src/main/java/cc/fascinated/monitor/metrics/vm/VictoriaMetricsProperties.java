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

    public String getDeleteSeriesUrl() {
        if (this.deleteSeriesUrl != null && !this.deleteSeriesUrl.isBlank()) {
            return this.deleteSeriesUrl;
        }
        if (this.importUrl.endsWith("/api/v1/import/prometheus")) {
            return this.importUrl.replace("/api/v1/import/prometheus", "/api/v1/admin/tsdb/delete_series");
        }
        if (this.importUrl.endsWith("/import/prometheus")) {
            return this.importUrl.replace("/import/prometheus", "/api/v1/admin/tsdb/delete_series");
        }
        int apiIndex = this.importUrl.indexOf("/api/");
        if (apiIndex > 0) {
            return this.importUrl.substring(0, apiIndex) + "/api/v1/admin/tsdb/delete_series";
        }
        return this.importUrl.replaceAll("/[^/]+$", "") + "/api/v1/admin/tsdb/delete_series";
    }
}
