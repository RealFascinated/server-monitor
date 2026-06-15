//go:build !linux

package gpu

import "fascinated.cc/monitor/agent/internal/ingest"

func Collect() []ingest.GPUMetric {
	return nil
}
