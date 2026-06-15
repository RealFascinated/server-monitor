//go:build linux

package cpu

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/linux"
)

type cpuEnergySource struct {
	paths    []string
	maxPaths []string
}

var (
	cpuEnergyOnce   sync.Once
	cachedCPUEnergy cpuEnergySource
	cpuEnergyFound  bool
)

// ReadCPUPackageEnergyMicrojoules returns the summed package energy counter in microjoules.
// maxMicrojoules is the wrap range when available (0 if unknown).
func ReadPackageEnergyMicrojoules() (energy uint64, maxMicrojoules uint64, ok bool) {
	cpuEnergyOnce.Do(func() {
		cachedCPUEnergy, cpuEnergyFound = discoverCPUEnergySource()
	})
	if !cpuEnergyFound {
		return 0, 0, false
	}
	return sumEnergy(cachedCPUEnergy)
}

// ComputePowerWatts derives average package power from two energy readings.
func ComputePowerWatts(before, after, maxMicrojoules uint64, elapsed time.Duration) (float64, bool) {
	if elapsed <= 0 {
		return 0, false
	}
	delta := energyDelta(before, after, maxMicrojoules)
	if delta == 0 {
		return 0, false
	}
	watts := float64(delta) / elapsed.Seconds() / 1_000_000
	if watts < 0 || watts > 10_000 {
		return 0, false
	}
	return watts, true
}

func discoverCPUEnergySource() (cpuEnergySource, bool) {
	if src, ok := discoverIntelRAPLEnergy(); ok {
		return src, true
	}
	if src, ok := discoverAMDEnergy(); ok {
		return src, true
	}
	return cpuEnergySource{}, false
}

func discoverIntelRAPLEnergy() (cpuEnergySource, bool) {
	root := linux.HostPath("/sys/class/powercap/intel-rapl")
	dirs, err := filepath.Glob(filepath.Join(root, "intel-rapl:*"))
	if err != nil {
		return cpuEnergySource{}, false
	}

	var src cpuEnergySource
	for _, dir := range dirs {
		nameBytes, err := os.ReadFile(filepath.Join(dir, "name"))
		if err != nil {
			continue
		}
		name := strings.TrimSpace(string(nameBytes))
		if !strings.HasPrefix(name, "package") {
			continue
		}
		energyPath := filepath.Join(dir, "energy_uj")
		if _, err := os.Stat(energyPath); err != nil {
			continue
		}
		src.paths = append(src.paths, energyPath)
		maxPath := filepath.Join(dir, "max_energy_range_uj")
		if _, err := os.Stat(maxPath); err == nil {
			src.maxPaths = append(src.maxPaths, maxPath)
		} else {
			src.maxPaths = append(src.maxPaths, "")
		}
	}
	if len(src.paths) == 0 {
		return cpuEnergySource{}, false
	}
	return src, true
}

func discoverAMDEnergy() (cpuEnergySource, bool) {
	dirs, err := filepath.Glob(linux.HostPath("/sys/class/hwmon/hwmon*"))
	if err != nil {
		return cpuEnergySource{}, false
	}

	var src cpuEnergySource
	for _, dir := range dirs {
		nameBytes, err := os.ReadFile(filepath.Join(dir, "name"))
		if err != nil {
			continue
		}
		name := strings.TrimSpace(string(nameBytes))
		if name != "amd_energy" {
			continue
		}
		labels, err := filepath.Glob(filepath.Join(dir, "energy*_label"))
		if err != nil {
			continue
		}
		for _, labelPath := range labels {
			labelBytes, err := os.ReadFile(labelPath)
			if err != nil {
				continue
			}
			label := strings.ToLower(strings.TrimSpace(string(labelBytes)))
			if !strings.Contains(label, "socket") {
				continue
			}
			base := strings.TrimSuffix(filepath.Base(labelPath), "_label")
			energyPath := filepath.Join(dir, base+"_input")
			if _, err := os.Stat(energyPath); err != nil {
				continue
			}
			src.paths = append(src.paths, energyPath)
			maxPath := filepath.Join(dir, base+"_max")
			if _, err := os.Stat(maxPath); err == nil {
				src.maxPaths = append(src.maxPaths, maxPath)
			} else {
				src.maxPaths = append(src.maxPaths, "")
			}
		}
		if len(src.paths) == 0 {
			if path, maxPath, ok := amdHighestSocketEnergy(dir); ok {
				src.paths = append(src.paths, path)
				src.maxPaths = append(src.maxPaths, maxPath)
			}
		}
	}
	if len(src.paths) == 0 {
		return cpuEnergySource{}, false
	}
	return src, true
}

func amdHighestSocketEnergy(hwmonDir string) (energyPath, maxPath string, ok bool) {
	inputs, err := filepath.Glob(filepath.Join(hwmonDir, "energy*_input"))
	if err != nil || len(inputs) == 0 {
		return "", "", false
	}
	bestIndex := -1
	for _, input := range inputs {
		base := filepath.Base(input)
		numStr := strings.TrimPrefix(strings.TrimSuffix(base, "_input"), "energy")
		idx, err := strconv.Atoi(numStr)
		if err != nil || idx <= bestIndex {
			continue
		}
		bestIndex = idx
		energyPath = input
		maxCandidate := filepath.Join(hwmonDir, "energy"+numStr+"_max")
		if _, err := os.Stat(maxCandidate); err == nil {
			maxPath = maxCandidate
		} else {
			maxPath = ""
		}
	}
	return energyPath, maxPath, bestIndex >= 0
}

func sumEnergy(src cpuEnergySource) (uint64, uint64, bool) {
	var total, maxTotal uint64
	for i, path := range src.paths {
		value, ok := readUint64File(path)
		if !ok {
			continue
		}
		total += value
		if i < len(src.maxPaths) && src.maxPaths[i] != "" {
			if max, ok := readUint64File(src.maxPaths[i]); ok {
				maxTotal += max
			}
		}
	}
	return total, maxTotal, total > 0
}

func energyDelta(before, after, max uint64) uint64 {
	if after >= before {
		return after - before
	}
	if max > 0 {
		return (max - before) + after
	}
	// 32-bit RAPL counters wrap at 2^32 microjoules on some AMD CPUs.
	return (1<<32 - before) + after
}

func readUint64File(path string) (uint64, bool) {
	data, err := os.ReadFile(path)
	if err != nil {
		return 0, false
	}
	value, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
	if err != nil {
		return 0, false
	}
	return value, true
}
