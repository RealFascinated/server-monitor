package lhm

import (
	"encoding/json"
	"fmt"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
)

type serverMetricsJSON struct {
	CPUTotalPercent *float64            `json:"cpuTotalPercent"`
	CPUPowerWatts   *float64            `json:"cpuPowerWatts"`
	Cores           []coreMetricJSON    `json:"cores"`
	Memory          memoryMetricJSON    `json:"memory"`
	Temperatures    []temperatureJSON   `json:"temperatures"`
	GPUs            []gpuMetricJSON     `json:"gpus"`
}

type gpuMetricJSON struct {
	DeviceID              string   `json:"deviceId"`
	Name                  string   `json:"name"`
	Vendor                string   `json:"vendor"`
	UsagePercent          *float64 `json:"usagePercent"`
	EncoderUsagePercent   *float64 `json:"encoderUsagePercent"`
	DecoderUsagePercent   *float64 `json:"decoderUsagePercent"`
	MemoryUsedBytes       *int64   `json:"memoryUsedBytes"`
	MemoryTotalBytes      *int64   `json:"memoryTotalBytes"`
	TemperatureCelsius    *float64 `json:"temperatureCelsius"`
	PowerWatts            *float64 `json:"powerWatts"`
}

type coreMetricJSON struct {
	CPU          string  `json:"cpu"`
	UsagePercent float64 `json:"usagePercent"`
}

type memoryMetricJSON struct {
	Used      *int64 `json:"used"`
	Available *int64 `json:"available"`
	Total     *int64 `json:"total"`
}

type temperatureJSON struct {
	Sensor  string  `json:"sensor"`
	Celsius float64 `json:"celsius"`
}

// ServerSnapshot is a parsed LHM helper response.
type ServerSnapshot struct {
	CPUTotalPercent *float64
	CPUPowerWatts   *float64
	Cores           []ingest.CPUCoreMetric
	Memory          MemorySnapshot
	Temperatures    []ingest.TemperatureMetric
	GPUs            []ingest.GPUMetric
}

// MemorySnapshot holds byte counts from LHM when available.
type MemorySnapshot struct {
	Used      *int64
	Available *int64
	Total     *int64
}

func (m MemorySnapshot) Complete() bool {
	return m.Used != nil && m.Available != nil && m.Total != nil
}

func ParseServerMetricsJSON(data []byte) (ServerSnapshot, error) {
	var raw serverMetricsJSON
	if err := json.Unmarshal(data, &raw); err != nil {
		return ServerSnapshot{}, fmt.Errorf("parse lhm json: %w", err)
	}
	return snapshotFromJSON(raw), nil
}

func snapshotFromJSON(raw serverMetricsJSON) ServerSnapshot {
	snap := ServerSnapshot{
		CPUTotalPercent: raw.CPUTotalPercent,
		CPUPowerWatts:   raw.CPUPowerWatts,
		Memory: MemorySnapshot{
			Used:      raw.Memory.Used,
			Available: raw.Memory.Available,
			Total:     raw.Memory.Total,
		},
	}
	if len(raw.Cores) > 0 {
		snap.Cores = make([]ingest.CPUCoreMetric, len(raw.Cores))
		for i, core := range raw.Cores {
			snap.Cores[i] = ingest.CPUCoreMetric{
				CPU:          core.CPU,
				UsagePercent: core.UsagePercent,
			}
		}
	}
	if len(raw.Temperatures) > 0 {
		snap.Temperatures = make([]ingest.TemperatureMetric, len(raw.Temperatures))
		for i, t := range raw.Temperatures {
			snap.Temperatures[i] = ingest.TemperatureMetric{
				Sensor:  t.Sensor,
				Celsius: t.Celsius,
			}
		}
	}
	if len(raw.GPUs) > 0 {
		snap.GPUs = make([]ingest.GPUMetric, 0, len(raw.GPUs))
		for _, g := range raw.GPUs {
			metric := ingest.GPUMetric{
				DeviceID: ingest.HashDeviceID(g.DeviceID),
				Name:     g.Name,
				Vendor:   g.Vendor,
			}
			if g.UsagePercent != nil {
				metric.UsagePercent = *g.UsagePercent
			}
			if strings.EqualFold(g.Vendor, "nvidia") {
				metric.EncoderUsagePercent = g.EncoderUsagePercent
				metric.DecoderUsagePercent = g.DecoderUsagePercent
			}
			if g.MemoryUsedBytes != nil {
				metric.MemoryUsedBytes = *g.MemoryUsedBytes
			}
			if g.MemoryTotalBytes != nil {
				metric.MemoryTotalBytes = *g.MemoryTotalBytes
			}
			if g.TemperatureCelsius != nil {
				metric.TemperatureCelsius = *g.TemperatureCelsius
			}
			if g.PowerWatts != nil {
				metric.PowerWatts = *g.PowerWatts
			}
			snap.GPUs = append(snap.GPUs, metric)
		}
	}
	return snap
}

// ApplyServerSnapshot copies LHM fields into metrics. Memory is applied only when complete.
func ApplyServerSnapshot(metrics *ingest.ServerMetrics, snap ServerSnapshot) {
	if snap.CPUTotalPercent != nil {
		metrics.CPUUsage = *snap.CPUTotalPercent
	}
	if snap.CPUPowerWatts != nil && *snap.CPUPowerWatts > 0 {
		metrics.CPUPowerWatts = *snap.CPUPowerWatts
	}
	if len(snap.Cores) > 0 {
		metrics.CPUCoreMetrics = snap.Cores
	}
	if len(snap.Temperatures) > 0 {
		metrics.TemperatureMetrics = snap.Temperatures
	}
	if snap.Memory.Complete() {
		metrics.MemoryUsage = float64(*snap.Memory.Used)
		metrics.MemoryAvailable = float64(*snap.Memory.Available)
		metrics.MemoryTotal = float64(*snap.Memory.Total)
	}
}
