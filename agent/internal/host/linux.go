//go:build linux

package host

import (
	"bufio"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"sync"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

var (
	physicalIDPattern = regexp.MustCompile(`^physical id\s*:\s*(\d+)`)
	coreIDPattern     = regexp.MustCompile(`^core id\s*:\s*(\d+)`)
	modelNamePattern  = regexp.MustCompile(`^model name\s*:\s*(.+)`)
	hardwarePattern   = regexp.MustCompile(`^Hardware\s*:\s*(.+)`)

	lscpuOnce sync.Once
	lscpuData map[string]string
)

func populatePlatformDetails(details ingest.ServerDetails) ingest.ServerDetails {
	if data, err := os.ReadFile("/etc/os-release"); err == nil {
		content := string(data)
		if name := ReadOSReleaseValue(content, "NAME"); name != "" {
			details.OsName = name
		}
		if version := ReadOSReleaseValue(content, "VERSION"); version != "" {
			details.OsVersion = version
		} else if versionID := ReadOSReleaseValue(content, "VERSION_ID"); versionID != "" {
			details.OsVersion = versionID
		}
	}

	if data, err := os.ReadFile("/proc/sys/kernel/osrelease"); err == nil {
		details.KernelVersion = strings.TrimSpace(string(data))
	}

	lscpu := getLscpuValues()
	details.CPUModel = linuxCPUModel(lscpu)
	details.SocketCount = linuxSocketCount(lscpu)
	details.CoreCount = linuxCoreCount(lscpu)
	details.ThreadCount = linuxThreadCount()
	return details
}

func LscpuInt(label string) int {
	if value := getLscpuValues()[label]; value != "" {
		if n, err := strconv.Atoi(value); err == nil {
			return n
		}
	}
	return 0
}

func getLscpuValues() map[string]string {
	lscpuOnce.Do(func() {
		out, err := exec.Command("lscpu").Output()
		if err != nil {
			lscpuData = map[string]string{}
			return
		}
		values := make(map[string]string)
		for line := range strings.SplitSeq(string(out), "\n") {
			parts := strings.SplitN(line, ":", 2)
			if len(parts) != 2 {
				continue
			}
			values[strings.TrimSpace(parts[0])] = strings.TrimSpace(parts[1])
		}
		lscpuData = values
	})
	return lscpuData
}

func linuxCPUModel(lscpu map[string]string) string {
	if model := lscpu["Model name"]; model != "" {
		return model
	}

	file, err := os.Open("/proc/cpuinfo")
	if err != nil {
		return "unknown"
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		if matches := modelNamePattern.FindStringSubmatch(line); len(matches) == 2 {
			return strings.TrimSpace(matches[1])
		}
		if matches := hardwarePattern.FindStringSubmatch(line); len(matches) == 2 {
			return strings.TrimSpace(matches[1])
		}
	}
	return "unknown"
}

func linuxSocketCount(lscpu map[string]string) int {
	if sockets := lscpu["Socket(s)"]; sockets != "" {
		if n, err := strconv.Atoi(sockets); err == nil && n > 0 {
			return n
		}
	}

	ids := make(map[string]struct{})
	file, err := os.Open("/proc/cpuinfo")
	if err != nil {
		return 1
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		if matches := physicalIDPattern.FindStringSubmatch(scanner.Text()); len(matches) == 2 {
			ids[matches[1]] = struct{}{}
		}
	}
	if len(ids) > 0 {
		return len(ids)
	}
	return 1
}

func linuxCoreCount(lscpu map[string]string) int {
	if linux.IsContainer() {
		if cores := uniqueCoreIDs(); cores > 0 {
			return cores
		}
		return linuxThreadCount()
	}

	coresPerSocket, _ := strconv.Atoi(lscpu["Core(s) per socket"])
	sockets, _ := strconv.Atoi(lscpu["Socket(s)"])
	if coresPerSocket > 0 && sockets > 0 {
		return coresPerSocket * sockets
	}
	return linuxThreadCount()
}

func linuxThreadCount() int {
	if count := runtime.NumCPU(); count > 0 {
		return count
	}
	return 1
}

func uniqueCoreIDs() int {
	ids := make(map[string]struct{})
	file, err := os.Open("/proc/cpuinfo")
	if err != nil {
		return 0
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		if matches := coreIDPattern.FindStringSubmatch(scanner.Text()); len(matches) == 2 {
			ids[matches[1]] = struct{}{}
		}
	}
	return len(ids)
}
