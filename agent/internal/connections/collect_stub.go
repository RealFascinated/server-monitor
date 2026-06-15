//go:build !linux

package connections

import "fascinated.cc/monitor/agent/internal/ingest"

func CollectTCP() []ingest.TCPConnectionMetric {
	return nil
}
