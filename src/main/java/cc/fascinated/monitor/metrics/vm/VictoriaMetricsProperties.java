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
}
