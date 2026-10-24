//go:build !linux

package docker

import "fascinated.cc/monitor/agent/internal/ingest"

func CollectContainerMetrics(allowCLIFallback bool) []ingest.DockerContainerMetric {
	return []ingest.DockerContainerMetric{}
}
