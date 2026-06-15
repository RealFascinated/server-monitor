package disk

import "fascinated.cc/monitor/agent/internal/ingest"

func UpdateUsageFromMounts(metrics []ingest.DiskMetric, mounts []Mount) {
	usageByName := make(map[string]Mount, len(mounts))
	for _, m := range mounts {
		usageByName[m.Name] = m
	}
	for i := range metrics {
		if m, ok := usageByName[metrics[i].DiskName]; ok {
			metrics[i].UsedBytes = int64(m.UsedBytes)
			metrics[i].TotalBytes = int64(m.TotalBytes)
			metrics[i].InodeUsed = int64(m.InodeUsed)
			metrics[i].InodeTotal = int64(m.InodeTotal)
		}
	}
}
