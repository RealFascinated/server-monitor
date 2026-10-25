package cc.fascinated.monitor.alert.engine;


/**
 * State of one rule against one series: whether it is firing and, for windowed rules, a small ring of
 * time buckets holding {@code (sum, count)} so the windowed average costs O(buckets) with no per-sample allocation.
 */
public final class SeriesState {
    static final int BUCKETS = 10;
    private static final double COVERAGE = 0.9;

    private final int bucketWidthSeconds;
    private final double[] sums;
    private final long[] counts;
    private final long[] ids;

    private boolean firing;
    private long firingSince;
    private double worstValue;
    private long lastSeen = Long.MIN_VALUE;
    private long firstSample = Long.MIN_VALUE;
    private long newestSample = Long.MIN_VALUE;
    private long newestBucket = Long.MIN_VALUE;

    public SeriesState(int windowSeconds) {
        if (windowSeconds > 0) {
            this.bucketWidthSeconds = Math.max(1, (windowSeconds + BUCKETS - 1) / BUCKETS);
            this.sums = new double[BUCKETS];
            this.counts = new long[BUCKETS];
            this.ids = new long[BUCKETS];
            java.util.Arrays.fill(this.ids, Long.MIN_VALUE);
        } else {
            this.bucketWidthSeconds = 0;
            this.sums = null;
            this.counts = null;
            this.ids = null;
        }
    }

    /** Seeds a firing state, used to restore open events after a restart. */
    public synchronized void restoreFiring(long since, double worst, long seenAt) {
        this.firing = true;
        this.firingSince = since;
        this.worstValue = worst;
        this.lastSeen = seenAt;
    }

    synchronized boolean isFiringInternal() {
        return this.firing;
    }

    public synchronized boolean isFiring() {
        return this.firing;
    }

    public synchronized long firingSince() {
        return this.firingSince;
    }

    public synchronized double worstValue() {
        return this.worstValue;
    }

    public synchronized long lastSeen() {
        return this.lastSeen;
    }

    /** Records a sample. Returns false when it is too old for the window and was ignored. */
    boolean add(long ts, double value) {
        this.lastSeen = Math.max(this.lastSeen, ts);
        if (this.sums == null) {
            return true;
        }
        long bucket = Math.floorDiv(ts, (long) this.bucketWidthSeconds);
        if (this.newestBucket != Long.MIN_VALUE && bucket <= this.newestBucket - BUCKETS) {
            return false;
        }
        int slot = (int) Math.floorMod(bucket, (long) BUCKETS);
        if (this.ids[slot] != bucket) {
            this.ids[slot] = bucket;
            this.sums[slot] = 0;
            this.counts[slot] = 0;
        }
        this.sums[slot] += value;
        this.counts[slot]++;
        this.newestBucket = Math.max(this.newestBucket, bucket);
        this.newestSample = Math.max(this.newestSample, ts);
        if (this.firstSample == Long.MIN_VALUE) {
            this.firstSample = ts;
        }
        return true;
    }

    double average(double latest) {
        if (this.sums == null) {
            return latest;
        }
        double sum = 0;
        long count = 0;
        for (int slot = 0; slot < BUCKETS; slot++) {
            if (this.ids[slot] != Long.MIN_VALUE && this.ids[slot] > this.newestBucket - BUCKETS) {
                sum += this.sums[slot];
                count += this.counts[slot];
            }
        }
        return count == 0 ? latest : sum / count;
    }

    boolean covered() {
        if (this.sums == null) {
            return true;
        }
        return this.firstSample != Long.MIN_VALUE
                && this.newestSample - this.firstSample >= COVERAGE * this.bucketWidthSeconds * BUCKETS;
    }

    void fire(long since, double value) {
        this.firing = true;
        this.firingSince = since;
        this.worstValue = value;
    }

    void resolve() {
        this.firing = false;
    }

    void updateWorst(double value) {
        this.worstValue = value;
    }
}
