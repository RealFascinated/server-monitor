package cc.fascinated.monitor.metrics.shared.support;

import java.util.concurrent.atomic.AtomicLong;

public final class CounterAccumulator {
    private final AtomicLong value = new AtomicLong();

    public void increment() {
        this.value.incrementAndGet();
    }

    public long get() {
        return this.value.get();
    }
}
