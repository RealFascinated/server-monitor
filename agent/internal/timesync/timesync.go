// Package timesync reports how well the host's own clock is disciplined. It reads what the host's
// time daemon or kernel already knows and sends no network traffic of its own.
package timesync

import (
	"sync"
	"time"
)

// Status is the host's time sync state. A nil field means the platform could not tell.
type Status struct {
	// Synced is whether the host believes its clock is synchronised to a time source.
	Synced *bool
	// ErrorMs is the kernel's own estimate of the clock error, in milliseconds.
	ErrorMs *float64
	// OffsetMs is the clock offset measured by the time daemon (positive: this clock is ahead).
	OffsetMs *float64
}

// refreshEvery keeps the daemon from being queried on every slow refresh.
const refreshEvery = time.Minute

var cache struct {
	mu     sync.Mutex
	at     time.Time
	status Status
}

// Read returns the host's time sync status, re-reading it at most once a minute.
func Read() Status {
	cache.mu.Lock()
	defer cache.mu.Unlock()
	if !cache.at.IsZero() && time.Since(cache.at) < refreshEvery {
		return cache.status
	}
	cache.status = read()
	cache.at = time.Now()
	return cache.status
}

func boolPtr(v bool) *bool        { return &v }
func floatPtr(v float64) *float64 { return &v }
