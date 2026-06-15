//go:build linux

package gpu

import "fascinated.cc/monitor/agent/internal/ingest"

func Collect() []ingest.GPUMetric {
	metrics := collectNVIDIA()
	metrics = append(metrics, collectDRM()...)
	if len(metrics) == 0 {
		return nil
	}
	return metrics
}
