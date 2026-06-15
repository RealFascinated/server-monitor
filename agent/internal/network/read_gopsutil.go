//go:build !linux

package network

import psutilnet "github.com/shirou/gopsutil/v4/net"

func ReadCounters() ([]Counter, error) {
	stats, err := psutilnet.IOCounters(true)
	if err != nil {
		return nil, err
	}

	counters := make([]Counter, len(stats))
	for i, stat := range stats {
		counters[i] = Counter{
			Name:        stat.Name,
			BytesRecv:   stat.BytesRecv,
			BytesSent:   stat.BytesSent,
			PacketsRecv: stat.PacketsRecv,
			PacketsSent: stat.PacketsSent,
			Errin:       stat.Errin,
			Errout:      stat.Errout,
		}
	}
	return counters, nil
}
