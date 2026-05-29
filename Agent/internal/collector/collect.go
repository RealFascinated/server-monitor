package collector

import (
	"fascinated.cc/monitor/agent/internal/ingest"
)

type Result struct {
	ServerMetrics    ingest.ServerMetrics
	InterfaceMetrics []ingest.InterfaceMetrics
	DiskMetrics      []ingest.DiskMetric
	ZfsArcMetrics    *ingest.ZFSArcMetrics
	ZfsPoolMetrics   []ingest.ZfsPoolMetric
	DockerContainers []ingest.DockerContainerMetric
}

func Collect(opts Options) (Result, error) {
	return collect(opts)
}
