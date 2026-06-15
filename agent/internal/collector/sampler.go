package collector

import (
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/platform"
)

// Sampler maintains stateful metric snapshots for background sampling and push.
type Sampler struct {
	opts Options

	mu     sync.RWMutex
	fastMu sync.Mutex
	slowMu sync.Mutex

	fast    FastSnapshot
	slow    SlowSnapshot
	backend platform.Backend
}

func NewSampler(opts Options) *Sampler {
	return &Sampler{
		opts: opts,
		fast: FastSnapshot{
			InterfaceMetrics: []ingest.InterfaceMetrics{},
			DiskMetrics:      []ingest.DiskMetric{},
		},
		backend: platform.New(platform.Options{
			HasZFS:       opts.HasZFS,
			EnableDocker: opts.EnableDocker,
			EnableGPU:    opts.EnableGPU,
		}),
	}
}

func (s *Sampler) Tick() error {
	s.fastMu.Lock()
	defer s.fastMu.Unlock()
	return s.tick()
}

func (s *Sampler) RefreshSlow() error {
	s.slowMu.Lock()
	defer s.slowMu.Unlock()
	return s.refreshSlow()
}

// RunOnce performs two ticks and one slow refresh for one-shot collection (print mode).
func (s *Sampler) RunOnce(interval time.Duration) error {
	if err := s.Tick(); err != nil {
		return err
	}
	time.Sleep(interval)
	if err := s.Tick(); err != nil {
		return err
	}
	return s.RefreshSlow()
}

func (s *Sampler) Ready() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.fast.Ready
}

func (s *Sampler) ClockMHz() float64 {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.fast.ClockMHz
}

// PushSnapshot returns assembled metrics and fast-path metadata for push.
func (s *Sampler) PushSnapshot() (ready bool, clockMHz float64, result Result) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.fast.Ready, s.fast.ClockMHz, Assemble(s.fast, s.slow)
}

// WaitReady blocks until at least one rate sample is available or the timeout elapses.
func (s *Sampler) WaitReady(timeout time.Duration) {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if s.Ready() {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
}
