//go:build !linux

package docker

import "fascinated.cc/monitor/agent/internal/ingest"

func CollectContainerMetrics() []ingest.DockerContainerMetric {
	return []ingest.DockerContainerMetric{}
}
