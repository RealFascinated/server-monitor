package battery

import "fascinated.cc/monitor/agent/internal/ingest"

type Snapshot struct {
	Percent float64
	OK      bool
}

func Read() Snapshot {
	return read()
}

func ApplyTo(metrics *ingest.ServerMetrics, s Snapshot) {
	if !s.OK {
		return
	}
	v := s.Percent
	metrics.BatteryPercent = &v
}
