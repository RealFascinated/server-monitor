package cc.fascinated.monitor.controller;

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class MetricsController {
    private static final PrometheusTextFormatWriter WRITER = PrometheusTextFormatWriter.create();

    private final PrometheusRegistry prometheusRegistry;

    public MetricsController(PrometheusRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public String metrics() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        WRITER.write(output, this.prometheusRegistry.scrape());
        return output.toString(StandardCharsets.UTF_8);
    }
}
