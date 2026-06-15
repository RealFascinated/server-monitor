package memory

import "fascinated.cc/monitor/agent/internal/ingest"

type Extras struct {
	Buffers, Cached, SwapUsed, SwapTotal int64
}

type Snapshot struct {
	Usage, Total, Available float64
	Extras                  Extras
}

func ApplyTo(metrics *ingest.ServerMetrics, s Snapshot) {
	metrics.MemoryUsage = s.Usage
	metrics.MemoryTotal = s.Total
	metrics.MemoryAvailable = s.Available
	metrics.MemoryBuffers = s.Extras.Buffers
	metrics.MemoryCached = s.Extras.Cached
	metrics.SwapUsed = s.Extras.SwapUsed
	metrics.SwapTotal = s.Extras.SwapTotal
}
