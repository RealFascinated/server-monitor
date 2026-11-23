//go:build !linux && !windows

package platform

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/reboot"
)

func New(opts Options) Backend {
	return &stubBackend{}
}

type stubBackend struct{}

func (stubBackend) Tick(bool) (TickUpdate, error) {
	required := reboot.Required()
	return TickUpdate{ServerMetrics: ingest.ServerMetrics{RebootRequired: &required}}, nil
}

func (stubBackend) RefreshSlow() (SlowUpdate, error) {
	return SlowUpdate{}, nil
}
