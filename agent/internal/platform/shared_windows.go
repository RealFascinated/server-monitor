//go:build windows

package platform

import (
	"sync/atomic"

	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/zfs"
)

type windowsBackendShared struct {
	mounts      atomic.Value // []disk.Mount
	poolStatus  atomic.Value // zfs.PoolStatusSnapshot
	poolIORates atomic.Value // map[string]zfs.PoolIORates
}

func newWindowsBackendShared() *windowsBackendShared {
	s := &windowsBackendShared{}
	s.mounts.Store([]disk.Mount(nil))
	s.poolStatus.Store(zfs.EmptyPoolStatusSnapshot())
	s.poolIORates.Store(map[string]zfs.PoolIORates{})
	return s
}

func (s *windowsBackendShared) loadMounts() []disk.Mount {
	if v := s.mounts.Load(); v != nil {
		return v.([]disk.Mount)
	}
	return nil
}

func (s *windowsBackendShared) storeMounts(mounts []disk.Mount) {
	s.mounts.Store(append([]disk.Mount(nil), mounts...))
}

func (s *windowsBackendShared) loadPoolStatus() zfs.PoolStatusSnapshot {
	if v := s.poolStatus.Load(); v != nil {
		return v.(zfs.PoolStatusSnapshot)
	}
	return zfs.EmptyPoolStatusSnapshot()
}

func (s *windowsBackendShared) storePoolStatus(status zfs.PoolStatusSnapshot) {
	s.poolStatus.Store(status)
}

func (s *windowsBackendShared) loadPoolIORates() map[string]zfs.PoolIORates {
	if v := s.poolIORates.Load(); v != nil {
		return v.(map[string]zfs.PoolIORates)
	}
	return map[string]zfs.PoolIORates{}
}

func (s *windowsBackendShared) storePoolIORates(rates map[string]zfs.PoolIORates) {
	copied := make(map[string]zfs.PoolIORates, len(rates))
	for k, v := range rates {
		copied[k] = v
	}
	s.poolIORates.Store(copied)
}
