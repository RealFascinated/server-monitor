package ingest

import (
	"sync"
	"time"
)

// Limits bound how much the push buffer may hold; the oldest samples are dropped first.
// A zero field means no limit of that kind.
type Limits struct {
	MaxAge     time.Duration
	MaxBytes   int
	MaxSamples int
}

type queuedSample struct {
	seq      uint64
	queuedAt time.Time
	gz       []byte // gzip-compressed JSON body of one Data sample
}

// QueueStats is a point-in-time view of the push buffer.
type QueueStats struct {
	Samples      int
	Bytes        int
	OldestAge    time.Duration
	DroppedTotal int64
}

// sampleQueue holds samples that have not reached the API yet, oldest first. Samples are kept
// compressed so a multi-hour outage costs a few MiB rather than tens.
type sampleQueue struct {
	now func() time.Time

	mu      sync.Mutex
	items   []queuedSample
	bytes   int
	nextSeq uint64
	limits  Limits
	dropped int64
}

func newSampleQueue(now func() time.Time) *sampleQueue {
	return &sampleQueue{now: now}
}

func (q *sampleQueue) setLimits(limits Limits) {
	q.mu.Lock()
	defer q.mu.Unlock()
	q.limits = limits
	q.evictLocked()
}

func (q *sampleQueue) add(body []byte) error {
	compressed, err := gzipBytes(body)
	if err != nil {
		return err
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	q.nextSeq++
	q.items = append(q.items, queuedSample{seq: q.nextSeq, queuedAt: q.now(), gz: compressed})
	q.bytes += len(compressed)
	q.evictLocked()
	return nil
}

func (q *sampleQueue) evictLocked() {
	if q.limits.MaxAge > 0 {
		cutoff := q.now().Add(-q.limits.MaxAge)
		for len(q.items) > 0 && q.items[0].queuedAt.Before(cutoff) {
			q.dropFirstLocked()
		}
	}
	for len(q.items) > 0 &&
		((q.limits.MaxBytes > 0 && q.bytes > q.limits.MaxBytes) ||
			(q.limits.MaxSamples > 0 && len(q.items) > q.limits.MaxSamples)) {
		q.dropFirstLocked()
	}
}

func (q *sampleQueue) dropFirstLocked() {
	q.bytes -= len(q.items[0].gz)
	q.items[0] = queuedSample{}
	q.items = q.items[1:]
	q.dropped++
}

// keepNewest discards everything but the newest n samples.
func (q *sampleQueue) keepNewest(n int) {
	q.mu.Lock()
	defer q.mu.Unlock()
	for len(q.items) > n {
		q.dropFirstLocked()
	}
}

// peek returns up to n of the oldest samples and the total number queued.
func (q *sampleQueue) peek(n int) ([]queuedSample, int) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if n > len(q.items) {
		n = len(q.items)
	}
	return append([]queuedSample(nil), q.items[:n]...), len(q.items)
}

// removeThrough discards every sample up to and including seq. Samples are identified by
// sequence so evictions that happen while a send is in flight cannot make us remove the wrong ones.
func (q *sampleQueue) removeThrough(seq uint64) {
	q.mu.Lock()
	defer q.mu.Unlock()
	for len(q.items) > 0 && q.items[0].seq <= seq {
		q.bytes -= len(q.items[0].gz)
		q.items[0] = queuedSample{}
		q.items = q.items[1:]
	}
}

// discardThrough is removeThrough for samples that are being given up on, so they are counted as dropped.
func (q *sampleQueue) discardThrough(seq uint64) {
	q.mu.Lock()
	defer q.mu.Unlock()
	for len(q.items) > 0 && q.items[0].seq <= seq {
		q.dropFirstLocked()
	}
}

func (q *sampleQueue) stats() QueueStats {
	q.mu.Lock()
	defer q.mu.Unlock()
	stats := QueueStats{Samples: len(q.items), Bytes: q.bytes, DroppedTotal: q.dropped}
	if len(q.items) > 0 {
		stats.OldestAge = q.now().Sub(q.items[0].queuedAt)
	}
	return stats
}
