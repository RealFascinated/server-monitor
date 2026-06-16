package cc.fascinated.monitor.support;

import java.time.Instant;

public final class TestFixtures {
    private TestFixtures() {}

    public static long[] metricWindow() {
        long to = Instant.now().getEpochSecond();
        return new long[]{to - 3600, to};
    }

    public static String minimalIngestPayload() {
        return """
                {
                  "agentVersion": "1.0.0",
                  "serverDetails": {
                    "ip": "127.0.0.1",
                    "osName": "Linux",
                    "osVersion": "6.0",
                    "kernelVersion": "6.0.0",
                    "cpuModel": "Test CPU",
                    "coreCount": 4,
                    "threadCount": 8,
                    "uptimeSeconds": 3600
                  },
                  "serverMetrics": {
                    "cpuUsage": 12.5,
                    "memoryUsage": 1024,
                    "memoryTotal": 8192
                  },
                  "interfaceMetrics": [],
                  "diskMetrics": []
                }
                """;
    }
}
