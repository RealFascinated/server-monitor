//go:build linux

package docker

import (
	"bufio"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

// ioCounters are cumulative per-container byte counters, as read from the kernel.
type ioCounters struct {
	rxBytes, txBytes            uint64
	blkReadBytes, blkWriteBytes uint64
	hasNet, hasBlock            bool
}

func readIOCounters(dir string) ioCounters {
	var counters ioCounters
	counters.blkReadBytes, counters.blkWriteBytes, counters.hasBlock = readCgroupBlockIO(dir)
	counters.rxBytes, counters.txBytes, counters.hasNet = readContainerNetwork(dir)
	return counters
}

// readCgroupBlockIO sums read and written bytes across every block device the container touched.
// cgroup v2 reports them in io.stat, cgroup v1 in blkio.throttle.io_service_bytes.
func readCgroupBlockIO(dir string) (read, write uint64, ok bool) {
	if file, err := os.Open(filepath.Join(dir, "io.stat")); err == nil {
		defer file.Close()
		scanner := bufio.NewScanner(file)
		for scanner.Scan() {
			fields := strings.Fields(scanner.Text())
			for _, field := range fields[min(1, len(fields)):] {
				key, value, found := strings.Cut(field, "=")
				if !found {
					continue
				}
				switch key {
				case "rbytes":
					read += parseUint(value)
				case "wbytes":
					write += parseUint(value)
				}
			}
		}
		return read, write, true
	}

	file, err := os.Open(filepath.Join(dir, "blkio.throttle.io_service_bytes"))
	if err != nil {
		return 0, 0, false
	}
	defer file.Close()
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		// "8:0 Read 4096"; the trailing "Total N" line has only two fields.
		fields := strings.Fields(scanner.Text())
		if len(fields) != 3 {
			continue
		}
		switch fields[1] {
		case "Read":
			read += parseUint(fields[2])
		case "Write":
			write += parseUint(fields[2])
		}
	}
	return read, write, true
}

// readContainerNetwork reads the container's traffic through the network namespace of its first
// process. It reports nothing for containers that share the agent's namespace (host networking),
// since those counters are the host's, and when the process is not visible to the agent.
func readContainerNetwork(dir string) (rx, tx uint64, ok bool) {
	pid := firstCgroupPID(dir)
	if pid == "" {
		return 0, 0, false
	}

	containerNS, err := os.Readlink(filepath.Join("/proc", pid, "ns", "net"))
	if err != nil {
		return 0, 0, false
	}
	if agentNS, err := os.Readlink("/proc/self/ns/net"); err != nil || agentNS == containerNS {
		return 0, 0, false
	}

	return readProcNetDev(linux.HostPath(filepath.Join("/proc", pid, "net", "dev")))
}

func firstCgroupPID(dir string) string {
	file, err := os.Open(filepath.Join(dir, "cgroup.procs"))
	if err != nil {
		return ""
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		pid := strings.TrimSpace(scanner.Text())
		// Processes outside the agent's PID namespace are listed as 0.
		if pid != "" && pid != "0" {
			if _, err := strconv.Atoi(pid); err == nil {
				return pid
			}
		}
	}
	return ""
}

// readProcNetDev totals received and transmitted bytes over every interface except loopback.
func readProcNetDev(path string) (rx, tx uint64, ok bool) {
	file, err := os.Open(path)
	if err != nil {
		return 0, 0, false
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		name, stats, found := strings.Cut(scanner.Text(), ":")
		if !found {
			continue // header lines
		}
		if strings.TrimSpace(name) == "lo" {
			continue
		}
		fields := strings.Fields(stats)
		if len(fields) < 9 {
			continue
		}
		rx += parseUint(fields[0])
		tx += parseUint(fields[8])
		ok = true
	}
	return rx, tx, ok
}

func parseUint(value string) uint64 {
	parsed, err := strconv.ParseUint(value, 10, 64)
	if err != nil {
		return 0
	}
	return parsed
}

// counterRate converts the change in a cumulative counter into bytes per second. It returns nil
// when there is no previous reading or the counter went backwards (the container restarted).
func counterRate(prev, current uint64, hadPrev bool, elapsedSeconds float64) *int64 {
	if !hadPrev || current < prev || elapsedSeconds <= 0 {
		return nil
	}
	rate := int64(float64(current-prev) / elapsedSeconds)
	return &rate
}
