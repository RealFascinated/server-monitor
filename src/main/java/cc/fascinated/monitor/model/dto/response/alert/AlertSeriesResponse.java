package cc.fascinated.monitor.model.dto.response.alert;

import java.util.List;
import java.util.Map;

/** Label sets currently reporting for a metric on a server (without server_id). */
public record AlertSeriesResponse(List<Map<String, String>> series) {}
