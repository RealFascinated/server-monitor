//go:build !linux

package metric

import (
	"math"

	"github.com/shirou/gopsutil/v4/cpu"
)

type CPUMetrics struct {
	Total  float64
	User   float64
	System float64
	Iowait float64
	Steal  float64
}

func ComputeCPUMetrics(before, after cpu.TimesStat) CPUMetrics {
	t1All, _ := getAllBusy(before)
	t2All, _ := getAllBusy(after)
	totalDelta := t2All - t1All
	if totalDelta <= 0 {
		return CPUMetrics{}
	}
	return CPUMetrics{
		Total:  calculateBusy(before, after),
		User:   clampPercent((after.User - before.User) / totalDelta * 100),
		System: clampPercent((after.System - before.System) / totalDelta * 100),
		Iowait: clampPercent((after.Iowait - before.Iowait) / totalDelta * 100),
		Steal:  clampPercent((after.Steal - before.Steal) / totalDelta * 100),
	}
}

func calculateBusy(t1, t2 cpu.TimesStat) float64 {
	t1All, t1Busy := getAllBusy(t1)
	t2All, t2Busy := getAllBusy(t2)

	if t2All <= t1All || t2Busy <= t1Busy {
		return 0
	}
	return clampPercent((t2Busy - t1Busy) / (t2All - t1All) * 100)
}

func getAllBusy(t cpu.TimesStat) (float64, float64) {
	tot := t.User + t.System + t.Idle + t.Nice + t.Iowait + t.Irq + t.Softirq + t.Steal + t.Guest + t.GuestNice
	busy := tot - t.Idle - t.Iowait
	return tot, busy
}

func clampPercent(value float64) float64 {
	return math.Min(100, math.Max(0, value))
}

func SocketCount(info []cpu.InfoStat) int {
	if len(info) == 0 {
		return 0
	}

	sockets := make(map[string]struct{})
	for _, c := range info {
		if c.PhysicalID != "" {
			sockets[c.PhysicalID] = struct{}{}
		}
	}
	if len(sockets) > 0 {
		return len(sockets)
	}
	return 1
}
