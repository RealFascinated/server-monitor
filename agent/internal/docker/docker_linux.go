//go:build linux

package docker

import (
	"bufio"
	"bytes"
	"encoding/json"
	"math"
	"regexp"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/executil"
	"fascinated.cc/monitor/agent/internal/host"
	"fascinated.cc/monitor/agent/internal/ingest"
)

type statsLine struct {
	Name     string `json:"Name"`
	CPUPerc  string `json:"CPUPerc"`
	MemUsage string `json:"MemUsage"`
}

var bytePattern = regexp.MustCompile(`^(-?[0-9.]+)(TiB|GiB|MiB|KiB|B|TB|GB|MB|KB)$`)

func CollectContainerMetrics() []ingest.DockerContainerMetric {
	if metrics := collectFromCgroups(); len(metrics) > 0 {
		return metrics
	}
	if !dockerCLIAvailable() {
		return []ingest.DockerContainerMetric{}
	}
	warnCgroupFallbackOnce()
	return collectFromDockerCLI()
}

func collectFromDockerCLI() []ingest.DockerContainerMetric {
	out, err := executil.CommandOutput("docker", "stats", "--format", "json", "--no-stream", "--no-trunc")
	if err != nil || len(bytes.TrimSpace(out)) == 0 {
		return []ingest.DockerContainerMetric{}
	}

	hostCPUs := hostCPUCount()
	if hostCPUs <= 0 {
		hostCPUs = 1
	}

	metrics := make([]ingest.DockerContainerMetric, 0)
	scanner := bufio.NewScanner(bytes.NewReader(out))
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		var stats statsLine
		if err := json.Unmarshal([]byte(line), &stats); err != nil {
			continue
		}
		if stats.Name == "" {
			continue
		}

		cpuUsage := normalizeCPU(stats.CPUPerc, hostCPUs)
		memoryUsage := parseMemoryUsage(stats.MemUsage)

		metrics = append(metrics, ingest.DockerContainerMetric{
			ContainerName: stats.Name,
			CPUUsage:      &cpuUsage,
			MemoryUsage:   memoryUsage,
		})
	}
	return metrics
}

func dockerCLIAvailable() bool {
	if _, err := executil.LookPath("docker"); err != nil {
		return false
	}
	_, err := executil.CommandOutput("docker", "info")
	return err == nil
}

func hostCPUCount() int {
	if out, err := executil.CommandOutput("docker", "info", "--format", "{{.NCPU}}"); err == nil {
		if n, err := strconv.Atoi(strings.TrimSpace(string(out))); err == nil && n > 0 {
			return n
		}
	}
	if value := host.LscpuInt("CPU(s)"); value > 0 {
		return value
	}
	return 1
}

func normalizeCPU(cpuPerc string, hostCPUs int) float64 {
	value := strings.TrimSpace(strings.TrimSuffix(cpuPerc, "%"))
	usage, err := strconv.ParseFloat(value, 64)
	if err != nil {
		return 0
	}
	usage = usage / float64(hostCPUs)
	if usage > 100 {
		usage = 100
	}
	if usage < 0 {
		usage = 0
	}
	return math.Round(usage*100) / 100
}

func parseMemoryUsage(memUsage string) int64 {
	parts := strings.Split(memUsage, " / ")
	if len(parts) == 0 {
		return 0
	}
	value := strings.ReplaceAll(strings.TrimSpace(parts[0]), " ", "")
	match := bytePattern.FindStringSubmatch(value)
	if len(match) != 3 {
		return 0
	}
	amount, err := strconv.ParseFloat(match[1], 64)
	if err != nil {
		return 0
	}
	multiplier := byteMultiplier(match[2])
	return int64(math.Round(amount * multiplier))
}

func byteMultiplier(unit string) float64 {
	switch unit {
	case "TiB":
		return 1099511627776
	case "GiB":
		return 1073741824
	case "MiB":
		return 1048576
	case "KiB":
		return 1024
	case "B":
		return 1
	case "TB":
		return 1000000000000
	case "GB":
		return 1000000000
	case "MB":
		return 1000000
	case "KB":
		return 1000
	default:
		return 0
	}
}
