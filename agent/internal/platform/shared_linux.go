//go:build linux

package platform

import (
	"sync/atomic"

	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/zfs"
)

type linuxBackendShared struct {
	mounts       atomic.Value // []disk.Mount
	poolStatus   atomic.Value // zfs.PoolStatusSnapshot
	poolIORates  atomic.Value // map[string]zfs.PoolIORates
	cgroupDevice atomic.Value // string
}

func newLinuxBackendShared() *linuxBackendShared {
	s := &linuxBackendShared{}
	s.mounts.Store([]disk.Mount(nil))
	s.poolStatus.Store(zfs.EmptyPoolStatusSnapshot())
	s.poolIORates.Store(map[string]zfs.PoolIORates{})
	s.cgroupDevice.Store("")
	return s
}

func (s *linuxBackendShared) loadMounts() []disk.Mount {
	if v := s.mounts.Load(); v != nil {
		return v.([]disk.Mount)
	}
	return nil
}

func (s *linuxBackendShared) storeMounts(mounts []disk.Mount) {
	s.mounts.Store(append([]disk.Mount(nil), mounts...))
}

func (s *linuxBackendShared) loadPoolStatus() zfs.PoolStatusSnapshot {
	if v := s.poolStatus.Load(); v != nil {
		return v.(zfs.PoolStatusSnapshot)
	}
	return zfs.EmptyPoolStatusSnapshot()
}

func (s *linuxBackendShared) storePoolStatus(status zfs.PoolStatusSnapshot) {
	s.poolStatus.Store(status)
}

func (s *linuxBackendShared) loadPoolIORates() map[string]zfs.PoolIORates {
	if v := s.poolIORates.Load(); v != nil {
		return v.(map[string]zfs.PoolIORates)
	}
	return map[string]zfs.PoolIORates{}
}

func (s *linuxBackendShared) storePoolIORates(rates map[string]zfs.PoolIORates) {
	copied := make(map[string]zfs.PoolIORates, len(rates))
	for k, v := range rates {
		copied[k] = v
	}
	s.poolIORates.Store(copied)
}

func (s *linuxBackendShared) loadCgroupDevice() string {
	if v := s.cgroupDevice.Load(); v != nil {
		return v.(string)
	}
	return ""
}

func (s *linuxBackendShared) storeCgroupDevice(device string) {
	s.cgroupDevice.Store(device)
}
