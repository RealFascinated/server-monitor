//go:build windows

package loadavg

import (
	"log/slog"
	"math"
	"runtime"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/winpdh"

	"golang.org/x/sys/windows"
)

const sampleInterval = 5 * time.Second

const (
	emaFactor1  = 0.9200444146293232
	emaFactor5  = 0.9834714538216175
	emaFactor15 = 0.9944598480048968
)

type pdhQuery struct {
	query            winpdh.Query
	queueCounter     windows.Handle
	processorCounter windows.Handle
}

type queueSampler struct {
	mu     sync.RWMutex
	load1  float64
	load5  float64
	load15 float64
	ready  bool
}

var sampler queueSampler

func init() {
	go runSampler()
}

func runSampler() {
	runtime.LockOSThread()
	defer runtime.UnlockOSThread()

	query, err := openLoadCounters()
	if err != nil {
		slog.Warn("load average sampler unavailable", "err", err)
		return
	}
	defer query.query.Close()

	query.query.Collect()
	time.Sleep(time.Second)
	query.query.Collect()

	sampleLoad(query)

	ticker := time.NewTicker(sampleInterval)
	defer ticker.Stop()
	for range ticker.C {
		sampleLoad(query)
	}
}

func sampleLoad(query *pdhQuery) {
	current, ok := collectLoadSample(query)
	if !ok {
		return
	}

	sampler.mu.Lock()
	defer sampler.mu.Unlock()

	if !sampler.ready {
		sampler.load1 = current
		sampler.load5 = current
		sampler.load15 = current
		sampler.ready = true
		return
	}

	sampler.load1 = sampler.load1*emaFactor1 + current*(1-emaFactor1)
	sampler.load5 = sampler.load5*emaFactor5 + current*(1-emaFactor5)
	sampler.load15 = sampler.load15*emaFactor15 + current*(1-emaFactor15)
}

func collectLoadSample(query *pdhQuery) (float64, bool) {
	query.query.Collect()

	queueLength, err := winpdh.ReadDouble(query.queueCounter)
	if err != nil {
		return 0, false
	}

	cpuPercent, err := winpdh.ReadDouble(query.processorCounter)
	if err != nil {
		return 0, false
	}

	cpus := runtime.NumCPU()
	if cpus <= 0 {
		cpus = 1
	}

	queueLoad := queueLength / float64(cpus)
	cpuLoad := (cpuPercent / 100.0) * float64(cpus)
	return math.Max(queueLoad, cpuLoad), true
}

func read() Averages {
	sampler.mu.RLock()
	defer sampler.mu.RUnlock()
	if !sampler.ready {
		return Averages{}
	}
	return Averages{
		Load1:  sampler.load1,
		Load5:  sampler.load5,
		Load15: sampler.load15,
	}
}

func openLoadCounters() (*pdhQuery, error) {
	query, err := winpdh.OpenQuery()
	if err != nil {
		return nil, err
	}

	queueCounter, err := query.AddCounter(`\System\Processor Queue Length`)
	if err != nil {
		query.Close()
		return nil, err
	}

	processorCounter, err := query.AddCounter(`\Processor(_Total)\% Processor Time`)
	if err != nil {
		query.Close()
		return nil, err
	}

	return &pdhQuery{
		query:            query,
		queueCounter:     queueCounter,
		processorCounter: processorCounter,
	}, nil
}
