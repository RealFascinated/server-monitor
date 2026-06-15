//go:build linux

package linux

import (
	"os"
	"path/filepath"
	"strings"
)

func mdArrayName(device string) string {
	if !strings.HasPrefix(device, "md") {
		return ""
	}
	if i := strings.IndexByte(device, 'p'); i > 2 {
		return device[:i]
	}
	return device
}

func mdDeviceHasDiskstatsIO(device string, stats map[string]DiskstatsEntry) bool {
	for _, name := range mdDiskstatsCandidates(device) {
		e, ok := stats[name]
		if !ok {
			continue
		}
		if e.Reads > 0 || e.Writes > 0 || e.SectorsRead > 0 || e.SectorsWritten > 0 || e.IoMs > 0 {
			return true
		}
	}
	return false
}

func mdDiskstatsCandidates(device string) []string {
	if md := mdArrayName(device); md != "" && md != device {
		return []string{device, md}
	}
	return []string{device}
}

func diskstatsDevicesForLookup(device string, stats map[string]DiskstatsEntry) []string {
	if md := mdArrayName(device); md != "" && !mdDeviceHasDiskstatsIO(device, stats) {
		if slaves := resolveMDSlaveDiskstatsNames(md, stats); len(slaves) > 0 {
			return slaves
		}
	}
	return []string{device}
}

func resolveMDSlaveDiskstatsNames(md string, diskstats map[string]DiskstatsEntry) []string {
	slaves, err := readMDSlaves(md)
	if err != nil || len(slaves) == 0 {
		return nil
	}

	names := make([]string, 0, len(slaves))
	for _, slave := range slaves {
		if name := blockDeviceInDiskstats(slave, diskstats); name != "" {
			names = append(names, name)
		}
	}
	return names
}

func readMDSlaves(md string) ([]string, error) {
	dir := HostPath(filepath.Join("/sys/block", md, "slaves"))
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, err
	}

	slaves := make([]string, 0, len(entries))
	for _, entry := range entries {
		slaves = append(slaves, entry.Name())
	}
	return slaves, nil
}

func blockDeviceInDiskstats(name string, diskstats map[string]DiskstatsEntry) string {
	if name == "" {
		return ""
	}
	if parent := diskstatsParentName(name); parent != "" {
		if _, ok := diskstats[parent]; ok {
			return parent
		}
	}
	if _, ok := diskstats[name]; ok {
		return name
	}
	return ""
}

func diskstatsParentName(name string) string {
	if nvmePartPattern.MatchString(name) {
		if parent := nvmePartPattern.FindStringSubmatch(name); len(parent) == 2 {
			return parent[1]
		}
	}
	if sdPartPattern.MatchString(name) {
		if parent := sdPartPattern.FindStringSubmatch(name); len(parent) == 2 {
			return parent[1]
		}
	}
	return ""
}
