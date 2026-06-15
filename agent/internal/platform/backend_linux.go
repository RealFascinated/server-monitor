//go:build linux

package platform

import (
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/connections"
	"fascinated.cc/monitor/agent/internal/cpu"
	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/docker"
	"fascinated.cc/monitor/agent/internal/fd"
	gpupkg "fascinated.cc/monitor/agent/internal/gpu"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
	"fascinated.cc/monitor/agent/internal/loadavg"
	"fascinated.cc/monitor/agent/internal/memory"
	"fascinated.cc/monitor/agent/internal/oom"
	"fascinated.cc/monitor/agent/internal/network"
	"fascinated.cc/monitor/agent/internal/thermal"
	"fascinated.cc/monitor/agent/internal/zfs"
)

type linuxTickState struct {
	lastAt time.Time

	cgroupCPUBefore uint64
	hasCgroupCPU    bool

	diskstatsBefore map[string]linux.DiskstatsEntry
	cgroupIOBefore  map[string]linux.CgroupIOEntry
	procBefore      linux.ProcStatSnapshot
	powerBefore     uint64
	powerMaxBefore  uint64
	hasPowerBefore  bool
	netBefore       []network.Counter
	zfsIOBefore     map[string]zfs.PoolIO
	arcBefore       zfs.ArcSnapshot
	hasArcBefore    bool

	oomKillsBefore uint64
	hasOomBefore   bool
}

type linuxBackend struct {
	opts Options

	state       linuxTickState
	initialized bool
	shared      *linuxBackendShared

	cgroup string
}

func newLinuxBackend(opts Options) *linuxBackend {
	return &linuxBackend{
		opts:   opts,
		shared: newLinuxBackendShared(),
		cgroup: linux.Dir(),
	}
}

func (b *linuxBackend) Tick(ready bool) (TickUpdate, error) {
	start := time.Now()

	var (
		procAfter      linux.ProcStatSnapshot
		netAfter       []network.Counter
		diskstatsAfter map[string]linux.DiskstatsEntry
		cgroupIOAfter  map[string]linux.CgroupIOEntry
		zfsIOAfter     map[string]zfs.PoolIO
		clockMHz       float64
		netErr         error
	)

	var wg sync.WaitGroup
	wg.Add(4)
	go func() {
		defer wg.Done()
		procAfter = linux.ReadProcStat()
	}()
	go func() {
		defer wg.Done()
		netAfter, netErr = network.ReadCounters()
	}()
	go func() {
		defer wg.Done()
		diskstatsAfter = linux.ReadDiskstats()
	}()
	go func() {
		defer wg.Done()
		if mhz, err := cpu.GetClockSpeedMHz(); err == nil {
			clockMHz = mhz
		}
	}()
	wg.Wait()
	profilePhase("parallel_reads", start)

	if netErr != nil {
		return TickUpdate{}, netErr
	}

	cgroupIOAfter = linux.ReadIOStats()
	powerAfter, powerMaxAfter, hasPowerAfter := cpu.ReadPackageEnergyMicrojoules()
	if b.opts.HasZFS {
		zfsIOAfter = zfs.ReadPoolIOSnapshots()
	}

	now := time.Now()
	if !b.initialized {
		b.state = linuxTickState{
			lastAt:          now,
			cgroupCPUBefore: readCgroupCPU(b.cgroup),
			hasCgroupCPU:    hasCgroupCPU(b.cgroup),
			diskstatsBefore: diskstatsAfter,
			cgroupIOBefore:  cgroupIOAfter,
			procBefore:      procAfter,
			powerBefore:     powerAfter,
			powerMaxBefore:  powerMaxAfter,
			hasPowerBefore:  hasPowerAfter,
			netBefore:       netAfter,
			zfsIOBefore:     zfsIOAfter,
		}
		if b.opts.HasZFS {
			b.state.arcBefore, b.state.hasArcBefore = zfs.ReadArcSnapshot()
		}
		b.initialized = true
		return TickUpdate{Skip: true}, nil
	}

	elapsed := now.Sub(b.state.lastAt)
	prev := b.state

	update := TickUpdate{Ready: true}
	if !ready {
		update.InterfaceMetrics = []ingest.InterfaceMetrics{}
		update.DiskMetrics = []ingest.DiskMetric{}
	}

	cpuStart := time.Now()
	metrics := cpu.ComputeLinuxTick(cpu.LinuxTickInput{
		PrevProc:       prev.procBefore,
		CurrProc:       procAfter,
		PrevPower:      prev.powerBefore,
		CurrPower:      powerAfter,
		PrevPowerMax:   prev.powerMaxBefore,
		CurrPowerMax:   powerMaxAfter,
		HasPowerBefore: prev.hasPowerBefore,
		HasPowerAfter:  hasPowerAfter,
		PrevCgroupCPU:  prev.cgroupCPUBefore,
		HasCgroupCPU:   prev.hasCgroupCPU,
		Cgroup:         b.cgroup,
		Elapsed:        elapsed,
	})
	profilePhase("tick_cpu", cpuStart)

	avg := loadavg.Read()
	metrics.Load1 = avg.Load1
	metrics.Load5 = avg.Load5
	metrics.Load15 = avg.Load15
	metrics.ProcessCount = avg.ProcessCount
	metrics.RunningProcesses = avg.RunningProcesses

	memory.ApplyTo(&metrics, memory.Read())
	fd.ApplyTo(&metrics, fd.Read())
	oomAfter, hasOom := oom.Read()
	if hasOom {
		oom.ApplyRate(&metrics, prev.oomKillsBefore, oomAfter, prev.hasOomBefore, elapsed)
	}

	update.InterfaceMetrics = network.ComputeMetrics(prev.netBefore, netAfter, elapsed)

	mounts := b.shared.loadMounts()
	if len(mounts) == 0 {
		if listed, err := disk.ListMounts(); err != nil {
			return TickUpdate{}, err
		} else {
			mounts = listed
			b.shared.storeMounts(mounts)
		}
	}

	poolStatus := b.shared.loadPoolStatus()
	vdevMap := poolStatus.VdevMap
	if vdevMap == nil {
		vdevMap = map[string][]string{}
	}

	diskStart := time.Now()
	poolIORates := zfs.ComputePoolIORates(prev.zfsIOBefore, zfsIOAfter, elapsed)
	b.shared.storePoolIORates(poolIORates)
	update.DiskMetrics = disk.BuildLinuxMetrics(
		mounts,
		prev.diskstatsBefore,
		diskstatsAfter,
		prev.cgroupIOBefore,
		cgroupIOAfter,
		prev.zfsIOBefore,
		zfsIOAfter,
		vdevMap,
		b.shared.loadCgroupDevice(),
		b.opts.HasZFS,
		elapsed,
	)
	profilePhase("tick_disk", diskStart)

	if b.opts.HasZFS {
		if arcAfter, ok := zfs.ReadArcSnapshot(); ok && prev.hasArcBefore {
			update.ZfsArcMetrics = zfs.ComputeArcMetrics(prev.arcBefore, arcAfter, elapsed)
		}
	}

	update.ServerMetrics = metrics
	update.ClockMHz = clockMHz

	b.state = linuxTickState{
		lastAt:          now,
		cgroupCPUBefore: readCgroupCPU(b.cgroup),
		hasCgroupCPU:    hasCgroupCPU(b.cgroup),
		diskstatsBefore: diskstatsAfter,
		cgroupIOBefore:  cgroupIOAfter,
		procBefore:      procAfter,
		powerBefore:     powerAfter,
		powerMaxBefore:  powerMaxAfter,
		hasPowerBefore:  hasPowerAfter,
		netBefore:       netAfter,
		zfsIOBefore:     zfsIOAfter,
	}
	if hasOom {
		b.state.oomKillsBefore = oomAfter
		b.state.hasOomBefore = true
	}
	if b.opts.HasZFS {
		b.state.arcBefore, b.state.hasArcBefore = zfs.ReadArcSnapshot()
	}

	profilePhase("tick_total", start)
	return update, nil
}

func (b *linuxBackend) RefreshSlow() (SlowUpdate, error) {
	start := time.Now()
	update := SlowUpdate{}

	if b.opts.HasZFS {
		zfsStart := time.Now()
		poolStatus := zfs.ReadPoolStatus()
		b.shared.storePoolStatus(poolStatus)
		cgroupDevice := resolveCgroupDevice()
		b.shared.storeCgroupDevice(cgroupDevice)
		update.ZfsPoolMetrics = zfs.CollectPoolMetrics(b.shared.loadPoolIORates(), poolStatus)
		profilePhase("slow_zfs", zfsStart)
	}

	if b.opts.EnableDocker {
		dockerStart := time.Now()
		update.DockerContainers = docker.CollectContainerMetrics()
		profilePhase("slow_docker", dockerStart)
	}
	if b.opts.EnableGPU {
		update.GPUMetrics = gpupkg.Collect()
	}

	tcpStart := time.Now()
	update.TCPConnectionMetrics = connections.CollectTCP()
	profilePhase("slow_tcp", tcpStart)

	update.ServerMetrics.TemperatureMetrics = thermal.ToIngest(thermal.ReadTemperatures())

	mounts, err := disk.ListMounts()
	if err == nil {
		b.shared.storeMounts(mounts)
		update.Mounts = mounts
		update.HasMounts = true
	}

	profilePhase("refresh_slow", start)
	return update, nil
}

func readCgroupCPU(cgroup string) uint64 {
	usage, _ := cpu.ReadCgroupUsageUsec(cgroup)
	return usage
}

func hasCgroupCPU(cgroup string) bool {
	_, ok := cpu.ReadCgroupUsageUsec(cgroup)
	return ok
}

func resolveCgroupDevice() string {
	cgroupIO := linux.ReadIOStats()
	for majmin := range cgroupIO {
		return linux.ResolveBlockDeviceName(majmin)
	}
	return ""
}
