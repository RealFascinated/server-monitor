//go:build !linux

package cpu

import (
	"sort"
	"strconv"
	"strings"

	"github.com/shirou/gopsutil/v4/cpu"
)

func ComputePerCoreCPUMetrics(before, after []cpu.TimesStat) []CoreUsage {
	beforeByCPU := indexPerCoreCPU(before)
	afterByCPU := indexPerCoreCPU(after)
	if len(beforeByCPU) == 0 || len(afterByCPU) == 0 {
		return nil
	}

	ids := make([]string, 0, len(afterByCPU))
	for id := range afterByCPU {
		if _, ok := beforeByCPU[id]; ok {
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
		out = append(out, CoreUsage{
			CPU:          id,
			UsagePercent: calculateBusy(beforeByCPU[id], afterByCPU[id]),
		})
	}
	return out
}

func indexPerCoreCPU(stats []cpu.TimesStat) map[string]cpu.TimesStat {
	indexed := make(map[string]cpu.TimesStat, len(stats))
	for _, stat := range stats {
		id, ok := perCoreCPUID(stat.CPU)
		if !ok {
			continue
		}
		indexed[id] = stat
	}
	return indexed
}

func perCoreCPUID(name string) (string, bool) {
	switch name {
	case "", "cpu-total", "all":
		return "", false
	}
	if strings.HasPrefix(name, "cpu") {
		id := strings.TrimPrefix(name, "cpu")
		if id != "" {
			return id, true
		}
	}
	return name, true
}
