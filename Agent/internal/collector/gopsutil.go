//go:build !linux

package collector

import (
	"time"

	"fascinated.cc/monitor/agent/internal/counters"
	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/docker"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
	"fascinated.cc/monitor/agent/internal/loadavg"
	"fascinated.cc/monitor/agent/internal/metric"
	"fascinated.cc/monitor/agent/internal/network"
	"fascinated.cc/monitor/agent/internal/sample"
	"fascinated.cc/monitor/agent/internal/zfs"

	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/mem"
)

type gopsutilOptions struct {
	enableIowaitSample bool
}

func collectGopsutil(opts Options, platformOpts gopsutilOptions) (Result, error) {
	result := Result{
		InterfaceMetrics: []ingest.InterfaceMetrics{},
		DiskMetrics:      []ingest.DiskMetric{},
		ZfsPoolMetrics:   []ingest.ZfsPoolMetric{},
	}

	var dockerCh chan []ingest.DockerContainerMetric
	if opts.EnableDocker {
		dockerCh = make(chan []ingest.DockerContainerMetric, 1)
		go func() {
			dockerCh <- docker.CollectContainerMetrics()
		}()
	}

	cpuBefore, err := cpu.Times(false)
	if err != nil || len(cpuBefore) == 0 {
		return result, err
	}

	netBefore, err := network.ReadCounters()
	if err != nil {
		return result, err
	}

	mounts, err := disk.ListMounts()
	if err != nil {
		return result, err
	}

	beforeIO, err := disk.ReadIOCounters()
	if err != nil {
		beforeIO = map[string]disk.IOCounters{}
	}

	countersBefore, countersPerSecond, _ := counters.ReadSystemCounters()

	var (
		beforeZFS    map[string]zfs.PoolIO
		zfsFuture    func() map[string]zfs.PoolIORates
		poolStatus   zfs.PoolStatusSnapshot
		arcBefore    zfs.ArcSnapshot
		hasArcBefore bool
	)

	if opts.HasZFS {
		poolStatus = zfs.ReadPoolStatus()
		arcBefore, hasArcBefore = zfs.ReadArcSnapshot()
	}
	if opts.HasZFS {
		beforeZFS = zfs.ReadPoolIOSnapshots()
		zfsFuture = zfs.StartPoolIostatSample()
	}

	if platformOpts.enableIowaitSample {
		metric.BeginIowaitSample()
	}

	sampleStart := time.Now()
	time.Sleep(sample.Interval)
	elapsed := time.Since(sampleStart)

	cpuAfter, err := cpu.Times(false)
	if err != nil || len(cpuAfter) == 0 {
		return result, err
	}
	cpuMetrics := metric.ComputeCPUMetrics(cpuBefore[0], cpuAfter[0])
	if platformOpts.enableIowaitSample {
		cpuMetrics.Iowait = metric.EndIowaitSample()
	}

	netAfter, err := network.ReadCounters()
	if err != nil {
		return result, err
	}

	afterIO, err := disk.ReadIOCounters()
	if err != nil {
		afterIO = map[string]disk.IOCounters{}
	}

	var afterZFS map[string]zfs.PoolIO
	var zfsRates map[string]zfs.PoolIORates
	if opts.HasZFS {
		afterZFS = zfs.ReadPoolIOSnapshots()
		if zfsFuture != nil {
			zfsRates = zfsFuture()
		}
	}

	metrics := ingest.ServerMetrics{
		CPUUsage:         cpuMetrics.Total,
		CPUUserPercent:   cpuMetrics.User,
		CPUSystemPercent: cpuMetrics.System,
		CPUIowaitPercent: cpuMetrics.Iowait,
		CPUStealPercent:  cpuMetrics.Steal,
	}

	if vm, err := mem.VirtualMemory(); err == nil {
		metrics.MemoryUsage = float64(vm.Used)
		metrics.MemoryAvailable = float64(vm.Available)
		metrics.MemoryTotal = float64(vm.Total)
		metrics.MemoryBuffers = int64(vm.Buffers)
		metrics.MemoryCached = int64(vm.Cached)
		metrics.SwapTotal = int64(vm.SwapTotal)
		metrics.SwapUsed = int64(vm.SwapTotal - vm.SwapFree)
	}

	load := loadavg.Read()
	metrics.Load1 = load.Load1
	metrics.Load5 = load.Load5
	metrics.Load15 = load.Load15

	metrics.ProcessCount, metrics.RunningProcesses = counters.ProcessStats()

	if countersPerSecond {
		metrics.ContextSwitchesPerSecond = int64(countersBefore.ContextSwitches)
		metrics.InterruptsPerSecond = int64(countersBefore.Interrupts)
	} else {
		countersAfter, _, _ := counters.ReadSystemCounters()
		metrics.ContextSwitchesPerSecond = iostats.PerSecond(delta.Uint64(countersAfter.ContextSwitches, countersBefore.ContextSwitches), elapsed)
		metrics.InterruptsPerSecond = iostats.PerSecond(delta.Uint64(countersAfter.Interrupts, countersBefore.Interrupts), elapsed)
	}

	if opts.HasZFS {
		if arcAfter, ok := zfs.ReadArcSnapshot(); ok && hasArcBefore {
			result.ZfsArcMetrics = zfs.ComputeArcMetrics(arcBefore, arcAfter, elapsed)
		}
		if zfsRates != nil {
			result.ZfsPoolMetrics = zfs.CollectPoolMetrics(zfsRates, poolStatus)
		}
	}

	if dockerCh != nil {
		result.DockerContainers = <-dockerCh
	}

	result.ServerMetrics = metrics
	result.InterfaceMetrics = network.ComputeMetrics(netBefore, netAfter, elapsed)
	result.DiskMetrics = disk.BuildFromSamples(opts.HasZFS, mounts, beforeIO, afterIO, beforeZFS, afterZFS, zfsRates, elapsed)
	return result, nil
}
