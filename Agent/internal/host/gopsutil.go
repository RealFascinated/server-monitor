//go:build !linux

package host

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/metric"

	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/host"
)

func populatePlatformDetails(details ingest.ServerDetails) ingest.ServerDetails {
	platform, _, version, _ := host.PlatformInformation()
	details.OsName = normalizeOSName(platform)
	details.OsVersion = version

	info, err := cpu.Info()
	if err == nil && len(info) > 0 {
		details.CPUModel = info[0].ModelName
	}
	details.SocketCount = metric.SocketCount(info)

	if cores, err := cpu.Counts(false); err == nil {
		details.CoreCount = cores
	}
	if threads, err := cpu.Counts(true); err == nil {
		details.ThreadCount = threads
	}
	return details
}

func normalizeOSName(platform string) string {
	if platform == "windows" {
		return "Windows"
	}
	return platform
}
