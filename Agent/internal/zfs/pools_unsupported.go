//go:build !linux

package zfs

import "fascinated.cc/monitor/agent/internal/ingest"

type PoolStatusSnapshot struct{}

func ReadPoolStatus() PoolStatusSnapshot {
	return PoolStatusSnapshot{}
}

func CollectPoolMetrics(_ map[string]PoolIORates, _ PoolStatusSnapshot) []ingest.ZfsPoolMetric {
	return []ingest.ZfsPoolMetric{}
}
