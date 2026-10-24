//go:build linux

package docker

import (
	"bufio"
	"log/slog"
	"math"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

type containerCgroup struct {
	id  string
	dir string
}

type cgroupCPUState struct {
	usageUsec uint64
	at        time.Time
	io        ioCounters
}

var cgroupCollector struct {
	mu    sync.Mutex
	prev  map[string]cgroupCPUState
	prevAt time.Time
}

func collectFromCgroups() []ingest.DockerContainerMetric {
	containers := discoverDockerCgroups()
	if len(containers) == 0 {
		return nil
	}

	hostCPUs := hostCPUCount()
	if hostCPUs <= 0 {
		hostCPUs = runtime.NumCPU()
	}
	if hostCPUs <= 0 {
		hostCPUs = 1
	}

	now := time.Now()
	cgroupCollector.mu.Lock()
	defer cgroupCollector.mu.Unlock()

	if cgroupCollector.prev == nil {
		cgroupCollector.prev = make(map[string]cgroupCPUState)
	}
	elapsed := now.Sub(cgroupCollector.prevAt)
	if cgroupCollector.prevAt.IsZero() || elapsed <= 0 {
		elapsed = time.Second
	}

	metrics := make([]ingest.DockerContainerMetric, 0, len(containers))
	next := make(map[string]cgroupCPUState, len(containers))

	for _, container := range containers {
		usageUsec, ok := readCgroupCPUUsageUsec(container.dir)
		if !ok {
			continue
		}
		counters := readIOCounters(container.dir)
		next[container.id] = cgroupCPUState{usageUsec: usageUsec, at: now, io: counters}

		var cpuUsage *float64
		if prev, found := cgroupCollector.prev[container.id]; found {
			if usageUsec >= prev.usageUsec {
				elapsedUsec := uint64(elapsed.Microseconds())
				if elapsedUsec > 0 {
					delta := usageUsec - prev.usageUsec
					if delta == 0 {
						zero := 0.0
						cpuUsage = &zero
					} else {
						usage := 100.0 * float64(delta) / float64(elapsedUsec) / float64(hostCPUs)
						if usage > 100 {
							usage = 100
						}
						usage = math.Round(usage*100) / 100
						cpuUsage = &usage
					}
				}
			}
		}

		metric := ingest.DockerContainerMetric{
			ContainerName: resolveContainerName(container.id),
			CPUUsage:      cpuUsage,
			MemoryUsage:   readCgroupMemoryBytes(container.dir),
		}
		if prev, found := cgroupCollector.prev[container.id]; found {
			applyIORates(&metric, prev, counters, now)
		}
		metrics = append(metrics, metric)
	}

	cgroupCollector.prev = next
	cgroupCollector.prevAt = now
	return metrics
}

// applyIORates fills the network and block I/O rates from two readings of the same container.
func applyIORates(metric *ingest.DockerContainerMetric, prev cgroupCPUState, current ioCounters, now time.Time) {
	seconds := now.Sub(prev.at).Seconds()
	if prev.io.hasNet && current.hasNet {
		metric.NetworkRxBytesPerSecond = counterRate(prev.io.rxBytes, current.rxBytes, true, seconds)
		metric.NetworkTxBytesPerSecond = counterRate(prev.io.txBytes, current.txBytes, true, seconds)
	}
	if prev.io.hasBlock && current.hasBlock {
		metric.BlockReadBytesPerSecond = counterRate(prev.io.blkReadBytes, current.blkReadBytes, true, seconds)
		metric.BlockWriteBytesPerSecond = counterRate(prev.io.blkWriteBytes, current.blkWriteBytes, true, seconds)
	}
}

func discoverDockerCgroups() []containerCgroup {
	root := linux.CgroupRoot()
	if root == "" {
		return nil
	}

	seen := make(map[string]struct{})
	var containers []containerCgroup

	add := func(id, dir string) {
		if id == "" || dir == "" {
			return
		}
		if _, ok := seen[id]; ok {
			return
		}
		if !cgroupHasMetrics(dir) {
			return
		}
		seen[id] = struct{}{}
		containers = append(containers, containerCgroup{id: id, dir: dir})
	}

	systemSlice := filepath.Join(root, "system.slice")
	if entries, err := os.ReadDir(systemSlice); err == nil {
		for _, entry := range entries {
			name := entry.Name()
			if !strings.HasPrefix(name, "docker-") || !strings.HasSuffix(name, ".scope") {
				continue
			}
			id := strings.TrimSuffix(strings.TrimPrefix(name, "docker-"), ".scope")
			add(id, filepath.Join(systemSlice, name))
		}
	}

	dockerDir := filepath.Join(root, "docker")
	if entries, err := os.ReadDir(dockerDir); err == nil {
		for _, entry := range entries {
			if !entry.IsDir() {
				continue
			}
			add(entry.Name(), filepath.Join(dockerDir, entry.Name()))
		}
	}

	return containers
}

func cgroupHasMetrics(dir string) bool {
	for _, name := range []string{"cpu.stat", "memory.current", "cpuacct.usage"} {
		if _, err := os.Stat(filepath.Join(dir, name)); err == nil {
			return true
		}
	}
	return false
}

func readCgroupCPUUsageUsec(dir string) (uint64, bool) {
	if data, err := os.ReadFile(filepath.Join(dir, "cpu.stat")); err == nil {
		for line := range strings.SplitSeq(string(data), "\n") {
			fields := strings.Fields(line)
			if len(fields) == 2 && fields[0] == "usage_usec" {
				value, err := strconv.ParseUint(fields[1], 10, 64)
				return value, err == nil
			}
		}
	}
	if data, err := os.ReadFile(filepath.Join(dir, "cpuacct.usage")); err == nil {
		nanos, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
		if err != nil {
			return 0, false
		}
		return nanos / 1000, true
	}
	return 0, false
}

func readCgroupMemoryBytes(dir string) int64 {
	current, ok := readUintFile(filepath.Join(dir, "memory.current"))
	if !ok {
		return 0
	}
	filePages, _ := readMemoryStatFile(dir, "file")
	if current > filePages {
		return int64(current - filePages)
	}
	return int64(current)
}

func readMemoryStatFile(dir, key string) (uint64, bool) {
	file, err := os.Open(filepath.Join(dir, "memory.stat"))
	if err != nil {
		return 0, false
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 2 && fields[0] == key {
			value, err := strconv.ParseUint(fields[1], 10, 64)
			return value, err == nil
		}
	}
	return 0, false
}

func readUintFile(path string) (uint64, bool) {
	data, err := os.ReadFile(path)
	if err != nil {
		return 0, false
	}
	value, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
	return value, err == nil
}

var cgroupFallbackWarn sync.Once

func warnCgroupFallbackOnce() {
	cgroupFallbackWarn.Do(func() {
		slog.Debug("docker cgroup collector found no containers, falling back to docker stats")
	})
}
