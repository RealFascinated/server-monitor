//go:build !linux

package disk

import (
	gopsutildisk "github.com/shirou/gopsutil/v4/disk"
)

func ReadIOCounters() (map[string]IOCounters, error) {
	stats, err := gopsutildisk.IOCounters()
	if err != nil {
		return nil, err
	}
	out := make(map[string]IOCounters, len(stats))
	for name, stat := range stats {
		out[name] = IOCounters{
			ReadBytes:  stat.ReadBytes,
			WriteBytes: stat.WriteBytes,
			ReadCount:  stat.ReadCount,
			WriteCount: stat.WriteCount,
			ReadTime:   stat.ReadTime,
			WriteTime:  stat.WriteTime,
			IoTime:     stat.IoTime,
		}
	}
	return out, nil
}
