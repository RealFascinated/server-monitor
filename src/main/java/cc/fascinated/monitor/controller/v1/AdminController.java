package cc.fascinated.monitor.controller.v1;

import cc.fascinated.monitor.model.domain.metric.MetricTimeRange;
import cc.fascinated.monitor.model.dto.response.metrics.MetricsResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.AdminMetricsService;
import cc.fascinated.monitor.web.auth.AuthenticatedAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/admin/metrics")
public class AdminController {
    private final AdminMetricsService adminMetricsService;

    public AdminController(AdminMetricsService adminMetricsService) {
        this.adminMetricsService = adminMetricsService;
    }

    @GetMapping
    public MetricsResponse getMetrics(
            @AuthenticatedAdmin UserRow admin,
            @RequestParam(defaultValue = "1h") String range
    ) {
        return this.adminMetricsService.getMetrics(MetricTimeRange.fromParam(range));
    }
}
