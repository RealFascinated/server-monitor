//go:build linux

package zfs

import (
	"bufio"
	"bytes"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

var (
	zfsPoolLinePattern    = regexp.MustCompile(`^\s*pool:\s*(.+)`)
	zfsDevPattern         = regexp.MustCompile(`/dev/\S+`)
	zfsScanPercentPattern = regexp.MustCompile(`([0-9]+(?:\.[0-9]+)?)%`)
)

type poolStatusInfo struct {
	pool      string
	scanState string
	scanPct   float64
	checksum  int64
}

type PoolStatusSnapshot struct {
	VdevMap   map[string][]string
	StatusMap map[string]poolStatusInfo
}

func ReadPoolStatus() PoolStatusSnapshot {
	empty := PoolStatusSnapshot{
		VdevMap:   map[string][]string{},
		StatusMap: map[string]poolStatusInfo{},
	}
	if _, err := exec.LookPath("zpool"); err != nil {
		return empty
	}

	out, err := exec.Command("zpool", "status", "-P").Output()
	if err != nil {
		return empty
	}

	vdevMap := make(map[string][]string)
	statusMap := make(map[string]poolStatusInfo)
	var current poolStatusInfo
	var pool string

	emit := func() {
		if current.pool != "" {
			statusMap[current.pool] = current
		}
	}

	scanner := bufio.NewScanner(bytes.NewReader(out))
	for scanner.Scan() {
		line := scanner.Text()
		if matches := zfsPoolLinePattern.FindStringSubmatch(line); len(matches) == 2 {
			emit()
			pool = strings.TrimSpace(matches[1])
			current = poolStatusInfo{pool: pool, scanState: "NONE"}
			continue
		}
		if pool == "" {
			continue
		}

		lower := strings.ToLower(line)
		if strings.Contains(lower, "scan:") {
			switch {
			case strings.Contains(lower, "scrub"):
				current.scanState = "SCRUB"
			case strings.Contains(lower, "resilver"):
				current.scanState = "RESILVER"
			default:
				current.scanState = "NONE"
			}
			if match := zfsScanPercentPattern.FindStringSubmatch(line); len(match) == 2 {
				current.scanPct, _ = strconv.ParseFloat(match[1], 64)
			}
			continue
		}

		devMatch := zfsDevPattern.FindString(line)
		if devMatch != "" {
			device := linux.ResolveDiskstatsDeviceName(devMatch, nil)
			if device == "" {
				path, err := filepath.EvalSymlinks(devMatch)
				if err != nil {
					path = devMatch
				}
				device = filepath.Base(path)
			}
			if device != "" {
				vdevMap[pool] = append(vdevMap[pool], device)
			}
			fields := strings.Fields(line)
			if len(fields) > 0 {
				if cksum, err := strconv.ParseInt(fields[len(fields)-1], 10, 64); err == nil {
					current.checksum += cksum
				}
			}
		}
	}
	emit()

	return PoolStatusSnapshot{VdevMap: vdevMap, StatusMap: statusMap}
}

func CollectPoolMetrics(ioRates map[string]PoolIORates, status PoolStatusSnapshot) []ingest.ZfsPoolMetric {
	if _, err := exec.LookPath("zpool"); err != nil {
		return []ingest.ZfsPoolMetric{}
	}

	listOut, err := exec.Command("zpool", "list", "-H", "-p", "-o", "name,size,alloc,free,cap,health,fragmentation").Output()
	if err != nil {
		return []ingest.ZfsPoolMetric{}
	}

	if status.StatusMap == nil {
		status = ReadPoolStatus()
	}

	metrics := make([]ingest.ZfsPoolMetric, 0)
	scanner := bufio.NewScanner(bytes.NewReader(listOut))
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 6 {
			continue
		}

		pool := fields[0]
		total := parseInt64(fields[1])
		alloc := parseInt64(fields[2])
		free := parseInt64(fields[3])
		capPct, _ := strconv.ParseFloat(fields[4], 64)
		health := fields[5]
		frag := 0.0
		if len(fields) >= 7 {
			frag, _ = strconv.ParseFloat(fields[6], 64)
		}

		info := status.StatusMap[pool]
		rates := ioRates[pool]

		metrics = append(metrics, ingest.ZfsPoolMetric{
			PoolName:             pool,
			Health:               health,
			CapacityPercent:      capPct,
			AllocatedBytes:       alloc,
			FreeBytes:            free,
			TotalBytes:           total,
			FragmentationPercent: frag,
			ScanState:            info.scanState,
			ScanPercent:          info.scanPct,
			ReadBps:              rates.ReadBytesPerSecond,
			WriteBps:             rates.WriteBytesPerSecond,
			ReadIops:             rates.ReadIops,
			WriteIops:            rates.WriteIops,
			ChecksumErrors:       info.checksum,
		})
	}
	return metrics
}

func parseInt64(value string) int64 {
	n, _ := strconv.ParseInt(value, 10, 64)
	return n
}
