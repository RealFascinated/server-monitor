package mdraid

import (
	"bufio"
	"io"
	"regexp"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
)

var (
	mdHeaderPattern  = regexp.MustCompile(`^(md[[:alnum:]_-]*)\s*:\s*(.*)$`)
	mdDevicePattern  = regexp.MustCompile(`^.+\[\d+\](\([A-Za-z]\))?$`)
	mdStatusCounts   = regexp.MustCompile(`\[(\d+)/(\d+)\]`)
	mdSyncAction     = regexp.MustCompile(`(?i)\b(resync|recovery|check|reshape)\s*=`)
	mdSyncPercent    = regexp.MustCompile(`(?i)\b(resync|recovery|check|reshape)\s*=\s*([0-9]+(?:\.[0-9]+)?)\s*%`)
	mdSyncSpeed      = regexp.MustCompile(`(?i)speed=([0-9]+)\s*K/sec`)
	mdSyncFinish     = regexp.MustCompile(`(?i)finish=([0-9]+(?:\.[0-9]+)?)\s*min`)
	mdSyncActionIdle = "idle"
)

// Parse reads an /proc/mdstat body and returns one metric per array. Lines for
// personalities and unused devices are ignored.
func Parse(r io.Reader) []ingest.MdArrayMetric {
	var metrics []ingest.MdArrayMetric
	var current *ingest.MdArrayMetric
	inactive := false
	fromStatus := false

	finalize := func() {
		if current == nil {
			return
		}
		if !fromStatus && !inactive {
			current.DisksActive = current.DisksTotal - current.DisksFailed
		}
		if inactive {
			current.DisksActive = 0
		}
		current.Degraded = current.DisksActive < current.DisksTotal
		metrics = append(metrics, *current)
		current = nil
		inactive = false
		fromStatus = false
	}

	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		if strings.HasPrefix(line, "Personalities") || strings.HasPrefix(line, "unused devices") {
			continue
		}

		if match := mdHeaderPattern.FindStringSubmatch(line); len(match) == 3 {
			finalize()
			fields := strings.Fields(match[2])
			current = parseHeader(match[1], fields)
			inactive = len(fields) > 0 && fields[0] == "inactive"
			continue
		}
		if current == nil {
			continue
		}

		if match := mdStatusCounts.FindStringSubmatch(line); len(match) == 3 {
			total, errTotal := strconv.ParseInt(match[1], 10, 64)
			active, errActive := strconv.ParseInt(match[2], 10, 64)
			if errTotal == nil && errActive == nil {
				current.DisksTotal = total
				current.DisksActive = active
				fromStatus = true
			}
			continue
		}

		if mdSyncAction.MatchString(line) {
			current.SyncAction = strings.ToLower(mdSyncAction.FindStringSubmatch(line)[1])
			if match := mdSyncPercent.FindStringSubmatch(line); len(match) == 3 {
				if pct, err := strconv.ParseFloat(match[2], 64); err == nil {
					current.SyncPercent = &pct
				}
			}
			if match := mdSyncSpeed.FindStringSubmatch(line); len(match) == 2 {
				if speed, err := strconv.ParseInt(match[1], 10, 64); err == nil {
					current.SyncSpeedKBps = &speed
				}
			}
			if match := mdSyncFinish.FindStringSubmatch(line); len(match) == 2 {
				if mins, err := strconv.ParseFloat(match[1], 64); err == nil {
					current.SyncFinishMinutes = &mins
				}
			}
		}
	}
	finalize()
	return metrics
}

func parseHeader(name string, rest []string) *ingest.MdArrayMetric {
	metric := &ingest.MdArrayMetric{Name: name, SyncAction: mdSyncActionIdle}
	devices := rest
	if len(rest) > 0 && rest[0] == "active" {
		devices = rest[1:]
		if len(devices) > 0 && !mdDevicePattern.MatchString(devices[0]) {
			metric.Level = devices[0]
			devices = devices[1:]
		}
	} else if len(rest) > 0 && rest[0] == "inactive" {
		devices = rest[1:]
	} else if len(rest) > 0 && !mdDevicePattern.MatchString(rest[0]) {
		metric.Level = rest[0]
		devices = rest[1:]
	}
	for _, device := range devices {
		if !mdDevicePattern.MatchString(device) {
			continue
		}
		metric.DisksTotal++
		if strings.HasSuffix(device, "(F)") {
			metric.DisksFailed++
		}
	}
	metric.DisksActive = metric.DisksTotal - metric.DisksFailed
	return metric
}
