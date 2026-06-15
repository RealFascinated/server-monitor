//go:build linux

package cpu

import (
	"math"
	"sort"
	"strconv"

	"fascinated.cc/monitor/agent/internal/linux"
)

func ComputePerCoreCPU(before, after map[string]linux.CPUStat) []CoreUsage {
	if len(before) == 0 || len(after) == 0 {
		return nil
	}

	ids := make([]string, 0, len(after))
	for id := range after {
		if _, ok := before[id]; ok {
			ids = append(ids, id)
		}
	}
	sort.Slice(ids, func(i, j int) bool {
		ai, aErr := strconv.Atoi(ids[i])
		aj, bErr := strconv.Atoi(ids[j])
		if aErr == nil && bErr == nil {
			return ai < aj
		}
		return ids[i] < ids[j]
	})

	out := make([]CoreUsage, 0, len(ids))
	for _, id := range ids {
		usage, _, _, _, _ := linux.ComputeCPUFromProcStat(before[id], after[id])
		out = append(out, CoreUsage{
			CPU:          id,
			UsagePercent: clampPercent(usage),
		})
	}
	return out
}

func clampPercent(value float64) float64 {
	return math.Min(100, math.Max(0, value))
}
