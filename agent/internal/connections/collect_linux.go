//go:build linux

package connections

import "fascinated.cc/monitor/agent/internal/ingest"

// CollectTCP returns TCP connection state counts from /proc/net/tcp.
func CollectTCP() []ingest.TCPConnectionMetric {
	return Read().ToIngest()
}
