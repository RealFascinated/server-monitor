//go:build !linux

package disk

import (
	"strings"

	gopsutildisk "github.com/shirou/gopsutil/v4/disk"
)

func listGopsutilMounts(include func(gopsutildisk.PartitionStat) bool) ([]Mount, error) {
	partitions, err := gopsutildisk.Partitions(false)
	if err != nil {
		return nil, err
	}

	mounts := make([]Mount, 0, len(partitions))
	for _, part := range partitions {
		if !include(part) {
			continue
		}

		usage, err := gopsutildisk.Usage(part.Mountpoint)
		if err != nil || usage.Total == 0 {
			continue
		}

		diskType := "block"
		if part.Fstype == "zfs" {
			diskType = "zfs"
		}

		mounts = append(mounts, Mount{
			Name:       part.Mountpoint,
			Source:     part.Device,
			Fstype:     part.Fstype,
			DiskType:   diskType,
			UsedBytes:  usage.Used,
			TotalBytes: usage.Total,
			InodeUsed:  usage.InodesUsed,
			InodeTotal: usage.InodesTotal,
		})
	}

	return dedupeMountsByUsage(mounts), nil
}

func includeUnixMount(part gopsutildisk.PartitionStat) bool {
	return part.Mountpoint != "" && !strings.HasPrefix(part.Mountpoint, "/dev")
}
