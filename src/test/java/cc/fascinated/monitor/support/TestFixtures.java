package cc.fascinated.monitor.support;

import java.time.Instant;

public final class TestFixtures {
    private TestFixtures() {}

    public static long[] metricWindow() {
        long to = Instant.now().getEpochSecond();
        return new long[]{to - 3600, to};
    }

    public static String ingestPayloadWithAgentMetrics() {
        return minimalIngestPayload().replace(
                "\"diskMetrics\": []",
                "\"diskMetrics\": [], \"agentMetrics\": {\"rssBytes\": 20000000, \"goroutines\": 14, "
                        + "\"heapInUseBytes\": 5000000, \"openFds\": 12, \"uptimeSeconds\": 90, "
                        + "\"lastPushDurationMs\": 40, \"lastPushPayloadBytes\": 2048, \"pushFailuresTotal\": 0, "
                        + "\"bufferedSamples\": 3, \"bufferedBytes\": 4096, \"bufferOldestAgeSeconds\": 15, "
                        + "\"bufferDroppedTotal\": 0}");
    }

    public static String ingestPayloadWithClockAndContainerIo() {
        return minimalIngestPayload()
                .replace("\"memoryTotal\": 8192",
                        "\"memoryTotal\": 8192, \"clockSynced\": true, \"clockErrorMs\": 1.5, \"clockOffsetMs\": -0.25")
                .replace("\"diskMetrics\": []",
                        "\"diskMetrics\": [], \"dockerContainers\": [{\"containerName\": \"web\", \"cpuUsage\": 1.5, "
                                + "\"memoryUsage\": 1024, \"networkRxBytesPerSecond\": 100, \"networkTxBytesPerSecond\": 50, "
                                + "\"blockReadBytesPerSecond\": 4096, \"blockWriteBytesPerSecond\": 0}]");
    }

    public static String ingestPayloadWithSmart() {
        return minimalIngestPayload().replace("\"diskMetrics\": []",
                "\"diskMetrics\": [], \"smartMetrics\": ["
                        + "{\"device\": \"sda\", \"model\": \"WDC WD40EFRX\", \"healthPassed\": true, "
                        + "\"temperatureCelsius\": 34, \"powerOnHours\": 41234, \"powerCycleCount\": 214, "
                        + "\"reallocatedSectors\": 8, \"reallocatedEvents\": 3, \"pendingSectors\": 0, "
                        + "\"reportedUncorrect\": 0, \"offlineUncorrectable\": 0, \"udmaCrcErrors\": 12, "
                        + "\"loadCycleCount\": 19042, \"startStopCounts\": 301, \"powerOffRetracts\": 41, "
                        + "\"heliumPercent\": 100, \"errorLogEntries\": 7, "
                        + "\"dataWrittenBytes\": 512000, \"dataReadBytes\": 1024000},"
                        + "{\"device\": \"sdf\", \"model\": \"Samsung SSD 870 EVO\", \"healthPassed\": true, "
                        + "\"powerOnHours\": 29788, \"enduranceUsedPercent\": 2, \"dataWrittenBytes\": 11874000000},"
                        + "{\"device\": \"nvme0\", \"model\": \"Samsung SSD 980 PRO\", \"healthPassed\": true, "
                        + "\"percentageUsed\": 4, \"availableSpare\": 96, \"mediaErrors\": 0, \"unsafeShutdowns\": 7, "
                        + "\"dataWrittenBytes\": 1024000000, \"dataReadBytes\": 512000000}]");
    }

    /** A sample as the agent's push buffer would send it: the normal payload plus its own timestamp. */
    public static String ingestPayloadAt(long timestamp) {
        return minimalIngestPayload().replaceFirst("\"agentVersion\": \"1.0.0\",",
                "\"agentVersion\": \"1.0.0\", \"timestamp\": " + timestamp + ",");
    }

    public static String batchPayload(Long sentAt, long... timestamps) {
        StringBuilder samples = new StringBuilder();
        for (int i = 0; i < timestamps.length; i++) {
            if (i > 0) {
                samples.append(',');
            }
            samples.append(ingestPayloadAt(timestamps[i]));
        }
        return "{" + (sentAt == null ? "" : "\"sentAt\": " + sentAt + ",") + "\"samples\": [" + samples + "]}";
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
