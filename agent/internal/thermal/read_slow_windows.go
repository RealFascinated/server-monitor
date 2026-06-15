//go:build windows

package thermal

import (
	"context"
	"log/slog"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/lhm"
)

func ReadSlow(ctx context.Context) ([]ingest.TemperatureMetric, error) {
	snap, err := lhm.GetServerMetrics(ctx)
	if err != nil {
		return nil, err
	}
	if len(snap.Temperatures) == 0 {
		return nil, nil
	}
	return snap.Temperatures, nil
}

var (
	lastLHMWarn   time.Time
	lhmWarnWindow = time.Minute
	lhmWarnMu     sync.Mutex
)

func WarnLHMSlow(err error) {
	lhmWarnMu.Lock()
	defer lhmWarnMu.Unlock()
	now := time.Now()
	if now.Sub(lastLHMWarn) < lhmWarnWindow {
		return
	}
	lastLHMWarn = now
	slog.Warn("lhm helper unavailable, keeping last slow metrics", "err", err)
}
