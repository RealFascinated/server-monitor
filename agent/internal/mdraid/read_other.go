//go:build !linux

package mdraid

import "fascinated.cc/monitor/agent/internal/ingest"

// Read returns no arrays on platforms without /proc/mdstat.
func Read() []ingest.MdArrayMetric {
	return nil
}
