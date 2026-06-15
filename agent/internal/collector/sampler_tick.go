package collector

import "fascinated.cc/monitor/agent/internal/platform"

func (s *Sampler) tick() error {
	ready := s.Ready()
	update, err := s.backend.Tick(ready)
	if err != nil {
		return err
	}
	if update.Skip {
		return nil
	}
	s.storeFast(update)
	return nil
}

func (s *Sampler) refreshSlow() error {
	update, err := s.backend.RefreshSlow()
	if err != nil {
		return err
	}
	s.storeSlow(update)
	return nil
}

func (s *Sampler) storeFast(update platform.TickUpdate) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.fast.Ready = update.Ready
	s.fast.ClockMHz = update.ClockMHz
	s.fast.ServerMetrics = update.ServerMetrics
	s.fast.InterfaceMetrics = update.InterfaceMetrics
	s.fast.DiskMetrics = update.DiskMetrics
	s.fast.ZfsArcMetrics = update.ZfsArcMetrics
}

func (s *Sampler) storeSlow(update platform.SlowUpdate) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if update.ZfsPoolMetrics != nil {
		s.slow.ZfsPoolMetrics = update.ZfsPoolMetrics
	}
	if update.DockerContainers != nil {
		s.slow.DockerContainers = update.DockerContainers
	}
	if update.GPUMetrics != nil {
		s.slow.GPUMetrics = update.GPUMetrics
	}
	if update.TCPConnectionMetrics != nil {
		s.slow.TCPConnectionMetrics = update.TCPConnectionMetrics
	}
	s.slow.ServerMetrics = update.ServerMetrics
	if update.HasMounts {
		s.slow.Mounts = update.Mounts
		s.slow.HasMounts = true
	}
}
