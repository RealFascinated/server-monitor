//go:build !linux

package host

import (
	cpupkg "fascinated.cc/monitor/agent/internal/cpu"
	"fascinated.cc/monitor/agent/internal/ingest"

	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/host"
)

func populatePlatformDetails(details ingest.ServerDetails) ingest.ServerDetails {
	platform, _, version, _ := host.PlatformInformation()
	details.OsName = normalizeOSName(platform)
	details.OsVersion = version

	if info, err := host.Info(); err == nil {
		details.KernelVersion = info.KernelVersion
	}

	info, err := cpu.Info()
	if err == nil && len(info) > 0 {
		details.CPUModel = info[0].ModelName
	}
	details.SocketCount = cpupkg.SocketCount(info)

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
