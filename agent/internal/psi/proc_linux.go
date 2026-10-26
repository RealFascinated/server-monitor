//go:build linux

package psi

import (
	"os"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

const microSecondsPerMilliSecond = 1000.0

type stall struct {
	Available          bool
	Avg10Pct, Avg60Pct float64
	Avg300Pct, TotalMs float64
}

type resource struct {
	Some, Full stall
}

func read() *ingest.PsiMetrics {
	cpu := readResource("cpu")
	memory := readResource("memory")
	io := readResource("io")
	if !cpu.available() && !memory.available() && !io.available() {
		return nil
	}

	return &ingest.PsiMetrics{
		CPUSomeAvg10Pct:     cpu.Some.avg10(),
		CPUSomeAvg60Pct:     cpu.Some.avg60(),
		CPUSomeAvg300Pct:    cpu.Some.avg300(),
		CPUSomeTotalMs:      cpu.Some.totalMs(),
		CPUFullAvg10Pct:     cpu.Full.avg10(),
		CPUFullAvg60Pct:     cpu.Full.avg60(),
		CPUFullAvg300Pct:    cpu.Full.avg300(),
		CPUFullTotalMs:      cpu.Full.totalMs(),
		MemorySomeAvg10Pct:  memory.Some.avg10(),
		MemorySomeAvg60Pct:  memory.Some.avg60(),
		MemorySomeAvg300Pct: memory.Some.avg300(),
		MemorySomeTotalMs:   memory.Some.totalMs(),
		MemoryFullAvg10Pct:  memory.Full.avg10(),
		MemoryFullAvg60Pct:  memory.Full.avg60(),
		MemoryFullAvg300Pct: memory.Full.avg300(),
		MemoryFullTotalMs:   memory.Full.totalMs(),
		IOSomeAvg10Pct:      io.Some.avg10(),
		IOSomeAvg60Pct:      io.Some.avg60(),
		IOSomeAvg300Pct:     io.Some.avg300(),
		IOSomeTotalMs:       io.Some.totalMs(),
		IOFullAvg10Pct:      io.Full.avg10(),
		IOFullAvg60Pct:      io.Full.avg60(),
		IOFullAvg300Pct:     io.Full.avg300(),
		IOFullTotalMs:       io.Full.totalMs(),
	}
}

func readResource(name string) resource {
	data, err := os.ReadFile(linux.HostPath("/proc/pressure/" + name))
	if err != nil {
		return resource{}
	}
	return parsePressure(string(data))
}

// parsePressure reads the "some" and "full" lines of a PSI file. A file may report no full line
// (cpu has historically exported only some), and unknown keys are ignored so newer kernels still
// parse.
func parsePressure(data string) resource {
	var out resource
	for _, line := range strings.Split(data, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}

		target := &out.Some
		switch fields[0] {
		case "full":
			target = &out.Full
		case "some":
		default:
			continue
		}

		var parsed stall
		averages := 0
		for _, field := range fields[1:] {
			key, raw, ok := strings.Cut(field, "=")
			if !ok {
				continue
			}
			value, err := strconv.ParseFloat(raw, 64)
			if err != nil {
				continue
			}
			switch key {
			case "avg10":
				parsed.Avg10Pct = value
				averages++
			case "avg60":
				parsed.Avg60Pct = value
				averages++
			case "avg300":
				parsed.Avg300Pct = value
				averages++
			case "total":
				parsed.TotalMs = value / microSecondsPerMilliSecond
			}
		}
		if averages != 3 {
			continue
		}

		parsed.Available = true
		*target = parsed
	}
	return out
}

func (r resource) available() bool {
	return r.Some.Available || r.Full.Available
}

func (s stall) avg10() *float64 {
	return stallValue(s, s.Avg10Pct)
}

func (s stall) avg60() *float64 {
	return stallValue(s, s.Avg60Pct)
}

func (s stall) avg300() *float64 {
	return stallValue(s, s.Avg300Pct)
}

func (s stall) totalMs() *float64 {
	return stallValue(s, s.TotalMs)
}

func stallValue(s stall, value float64) *float64 {
	if !s.Available {
		return nil
	}
	return ingest.FloatPtr(value)
}
