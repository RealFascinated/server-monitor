//go:build windows

package disk

import (
	"strings"

	gopsutildisk "github.com/shirou/gopsutil/v4/disk"
	"golang.org/x/sys/windows"
)

func ListMounts() ([]Mount, error) {
	return listGopsutilMounts(shouldIncludeWindowsMount)
}

func shouldIncludeWindowsMount(part gopsutildisk.PartitionStat) bool {
	if part.Mountpoint == "" {
		return false
	}
	if part.Fstype == "UDF" || part.Fstype == "CDFS" {
		return false
	}

	lower := strings.ToLower(part.Mountpoint)
	if strings.Contains(lower, "removable") {
		return false
	}
	if len(part.Mountpoint) < 2 || part.Mountpoint[1] != ':' {
		return false
	}

	path, err := windows.UTF16PtrFromString(part.Mountpoint)
	if err != nil {
		return false
	}
	return windows.GetDriveType(path) == windows.DRIVE_FIXED
}

func resolveDiskDevice(source string, beforeIO, afterIO map[string]IOCounters) string {
	device := strings.TrimSuffix(source, `\`)
	device = strings.TrimSuffix(device, `:`) + ":"
	if hasDiskDevice(device, beforeIO, afterIO) {
		return device
	}
	if hasDiskDevice(source, beforeIO, afterIO) {
		return source
	}
	return ""
}
