//go:build linux

package gpu

import (
	"bufio"
	"bytes"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/executil"
	"fascinated.cc/monitor/agent/internal/ingest"
)

const nvidiaQuery = "name,uuid,utilization.gpu,utilization.memory,temperature.gpu,memory.used,memory.total,power.draw,utilization.encoder,utilization.decoder"

func collectNVIDIA() []ingest.GPUMetric {
	if _, err := executil.LookPath("nvidia-smi"); err != nil {
		return nil
	}

	out, err := executil.CommandOutput(
		"nvidia-smi",
		"--query-gpu="+nvidiaQuery,
		"--format=csv,noheader,nounits",
	)
	if err != nil || len(bytes.TrimSpace(out)) == 0 {
		return nil
	}

	var metrics []ingest.GPUMetric
	scanner := bufio.NewScanner(bytes.NewReader(out))
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		if metric, ok := parseNVIDIALine(line); ok {
			metrics = append(metrics, metric)
		}
	}
	if len(metrics) == 0 {
		return nil
	}
	return metrics
}

func parseNVIDIALine(line string) (ingest.GPUMetric, bool) {
	fields := splitCSVFields(line)
	if len(fields) < 8 {
		return ingest.GPUMetric{}, false
	}

	rawID := strings.TrimSpace(fields[1])
	metric := ingest.GPUMetric{
		DeviceID: ingest.HashDeviceID(rawID),
		Name:     strings.TrimSpace(fields[0]),
		Vendor:   "nvidia",
	}
	if v, ok := parseOptionalFloat(fields[2]); ok {
		metric.UsagePercent = v
	}
	if usedMiB, ok := parseOptionalInt64(fields[5]); ok {
		metric.MemoryUsedBytes = usedMiB * 1024 * 1024
	}
	if totalMiB, ok := parseOptionalInt64(fields[6]); ok {
		metric.MemoryTotalBytes = totalMiB * 1024 * 1024
	}
	if v, ok := parseOptionalFloat(fields[4]); ok {
		metric.TemperatureCelsius = v
	}
	if v, ok := parseOptionalFloat(fields[7]); ok {
		metric.PowerWatts = v
	}
	if len(fields) >= 10 {
		if v, ok := parseOptionalFloat(fields[8]); ok {
			metric.EncoderUsagePercent = ingest.FloatPtr(v)
		}
		if v, ok := parseOptionalFloat(fields[9]); ok {
			metric.DecoderUsagePercent = ingest.FloatPtr(v)
		}
	}
	return metric, metric.Name != "" && rawID != ""
}

func splitCSVFields(line string) []string {
	var fields []string
	var current strings.Builder
	inQuotes := false
	for _, ch := range line {
		switch ch {
		case '"':
			inQuotes = !inQuotes
		case ',':
			if inQuotes {
				current.WriteRune(ch)
			} else {
				fields = append(fields, current.String())
				current.Reset()
			}
		default:
			current.WriteRune(ch)
		}
	}
	fields = append(fields, current.String())
	return fields
}

func parseOptionalFloat(value string) (float64, bool) {
	value = strings.TrimSpace(value)
	if value == "" || strings.EqualFold(value, "[N/A]") {
		return 0, false
	}
	parsed, err := strconv.ParseFloat(value, 64)
	if err != nil {
		return 0, false
	}
	return parsed, true
}

func parseOptionalInt64(value string) (int64, bool) {
	value = strings.TrimSpace(value)
	if value == "" || strings.EqualFold(value, "[N/A]") {
		return 0, false
	}
	parsed, err := strconv.ParseInt(value, 10, 64)
	if err != nil {
		return 0, false
	}
	return parsed, true
}
