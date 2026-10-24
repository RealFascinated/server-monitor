package ingest

import (
	"testing"
	"time"
)

type fakeClock struct{ t time.Time }

func (c *fakeClock) now() time.Time          { return c.t }
func (c *fakeClock) advance(d time.Duration) { c.t = c.t.Add(d) }

func newTestQueue(limits Limits) (*sampleQueue, *fakeClock) {
	clock := &fakeClock{t: time.Unix(1_700_000_000, 0)}
	q := newSampleQueue(clock.now)
	q.setLimits(limits)
	return q, clock
}

func mustAdd(t *testing.T, q *sampleQueue, body string) {
	t.Helper()
	if err := q.add([]byte(body)); err != nil {
		t.Fatalf("add: %v", err)
	}
}

func TestQueueEvictsOldestBySampleCount(t *testing.T) {
	q, _ := newTestQueue(Limits{MaxSamples: 3})
	for _, body := range []string{`{"n":1}`, `{"n":2}`, `{"n":3}`, `{"n":4}`, `{"n":5}`} {
		mustAdd(t, q, body)
	}

	stats := q.stats()
	if stats.Samples != 3 || stats.DroppedTotal != 2 {
		t.Fatalf("stats = %+v, want 3 samples and 2 dropped", stats)
	}
	items, _ := q.peek(10)
	first, err := gunzip(items[0].gz)
	if err != nil || string(first) != `{"n":3}` {
		t.Fatalf("oldest kept = %q (%v), want n=3", first, err)
	}
}

func TestQueueEvictsByBytes(t *testing.T) {
	q, _ := newTestQueue(Limits{})
	mustAdd(t, q, `{"n":1}`)
	one := q.stats().Bytes

	q.setLimits(Limits{MaxBytes: one*2 + one/2})
	for i := 0; i < 5; i++ {
		mustAdd(t, q, `{"n":1}`)
	}

	stats := q.stats()
	if stats.Samples != 2 || stats.Bytes > one*3 {
		t.Fatalf("stats = %+v, want 2 samples within the byte cap", stats)
	}
}

func TestQueueDropsSampleLargerThanByteCap(t *testing.T) {
	q, _ := newTestQueue(Limits{MaxBytes: 4})
	mustAdd(t, q, `{"a":"a sample that cannot possibly fit"}`)

	if stats := q.stats(); stats.Samples != 0 || stats.Bytes != 0 || stats.DroppedTotal != 1 {
		t.Fatalf("stats = %+v, want the oversized sample dropped", stats)
	}
}

func TestQueueEvictsByAge(t *testing.T) {
	q, clock := newTestQueue(Limits{MaxAge: time.Hour})
	mustAdd(t, q, `{"n":1}`)
	clock.advance(40 * time.Minute)
	mustAdd(t, q, `{"n":2}`)
	clock.advance(30 * time.Minute)
	mustAdd(t, q, `{"n":3}`)

	stats := q.stats()
	if stats.Samples != 2 || stats.DroppedTotal != 1 {
		t.Fatalf("stats = %+v, want first sample aged out", stats)
	}
	if stats.OldestAge != 30*time.Minute {
		t.Fatalf("oldest age = %v, want 30m", stats.OldestAge)
	}
}

func TestQueueRemoveThroughIgnoresEvictedSamples(t *testing.T) {
	q, _ := newTestQueue(Limits{MaxSamples: 3})
	mustAdd(t, q, `{"n":1}`)
	mustAdd(t, q, `{"n":2}`)
	inFlight, _ := q.peek(2)

	// New samples arrive while the batch is being sent and push the oldest out.
	mustAdd(t, q, `{"n":3}`)
	mustAdd(t, q, `{"n":4}`)
	q.removeThrough(inFlight[1].seq)

	items, total := q.peek(10)
	if total != 2 {
		t.Fatalf("queue holds %d samples, want the 2 that were never sent", total)
	}
	if body, _ := gunzip(items[0].gz); string(body) != `{"n":3}` {
		t.Fatalf("first remaining = %s, want n=3", body)
	}
}

func TestQueueKeepNewest(t *testing.T) {
	q, _ := newTestQueue(Limits{})
	for _, body := range []string{`{"n":1}`, `{"n":2}`, `{"n":3}`} {
		mustAdd(t, q, body)
	}
	q.keepNewest(1)

	items, total := q.peek(10)
	if total != 1 {
		t.Fatalf("queue holds %d samples, want 1", total)
	}
	if body, _ := gunzip(items[0].gz); string(body) != `{"n":3}` {
		t.Fatalf("kept = %s, want n=3", body)
	}
}
