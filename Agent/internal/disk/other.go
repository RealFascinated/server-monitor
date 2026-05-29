//go:build !linux && !windows

package disk

import "strings"

func ListMounts() ([]Mount, error) {
	return listGopsutilMounts(includeUnixMount)
}

func resolveDiskDevice(source string, beforeIO, afterIO map[string]IOCounters) string {
	base := source
	if i := strings.LastIndex(source, "/"); i >= 0 {
		base = source[i+1:]
	}
	if hasDiskDevice(base, beforeIO, afterIO) {
		return base
	}
	if hasDiskDevice(source, beforeIO, afterIO) {
		return source
	}
	return ""
}
