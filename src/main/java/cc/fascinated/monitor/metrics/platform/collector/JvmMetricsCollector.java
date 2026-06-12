package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import com.sun.management.UnixOperatingSystemMXBean;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

@Component
public class JvmMetricsCollector {

    public void write(PrometheusWriteContext ctx) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        ctx.gauge(PlatformMetricFamily.JVM_HEAP_USED_BYTES.metricName(), memory.getHeapMemoryUsage().getUsed());
        ctx.gauge(PlatformMetricFamily.JVM_HEAP_MAX_BYTES.metricName(), memory.getHeapMemoryUsage().getMax());
        ctx.gauge(PlatformMetricFamily.JVM_NONHEAP_USED_BYTES.metricName(), memory.getNonHeapMemoryUsage().getUsed());
        ctx.gauge(PlatformMetricFamily.JVM_THREAD_COUNT.metricName(), threads.getThreadCount());
        ctx.gauge(PlatformMetricFamily.JVM_UPTIME_SECONDS.metricName(), ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);

        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            double cpuLoad = sunOs.getProcessCpuLoad();
            if (cpuLoad >= 0) {
                ctx.gauge(PlatformMetricFamily.JVM_PROCESS_CPU_LOAD.metricName(), cpuLoad);
            }
        }
        if (osBean instanceof UnixOperatingSystemMXBean unixOs) {
            long rss = residentSetSizeBytes(unixOs);
            if (rss > 0) {
                ctx.gauge(PlatformMetricFamily.JVM_PROCESS_RSS_BYTES.metricName(), rss);
            }
        }
    }

    private static long residentSetSizeBytes(UnixOperatingSystemMXBean unixOs) {
        try {
            var method = unixOs.getClass().getMethod("getResidentSetSize");
            return ((Number) method.invoke(unixOs)).longValue();
        } catch (ReflectiveOperationException ex) {
            return -1;
        }
    }
}
