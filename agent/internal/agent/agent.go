package agent

import (
	"context"
	"log/slog"
	"os"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/collector"
	"fascinated.cc/monitor/agent/internal/host"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/zfs"
	"github.com/robfig/cron/v3"
)

const (
	DefaultVersion      = "2.0.0"
	heartbeatInterval   = 30 * time.Second
	sampleInterval      = time.Second
	slowMetricsInterval = 30 * time.Second
)

type Agent struct {
	Version       string
	Config        *Config
	ServerDetails ingest.ServerDetails
	HasZFS        bool

	sampler *collector.Sampler

	configMu sync.RWMutex
	cronMu   sync.Mutex
	cron     *cron.Cron
}

type Config = ingest.Config

func New(config *Config, version string) *Agent {
	if version == "" {
		version = DefaultVersion
	}
	details, hasZFS := host.PopulateDetails()
	return &Agent{
		Version:       version,
		Config:        config,
		ServerDetails: details,
		HasZFS:        hasZFS,
	}
}

func (a *Agent) Run(ctx context.Context) {
	a.refreshSampler()

	schedule := a.pushSchedule()
	if err := a.startCron(schedule); err != nil {
		slog.Error("start push schedule", "schedule", schedule, "err", err)
		return
	}
	defer a.stopCron()

	slog.Info("agent started", "schedule", schedule)

	go a.runSampleLoop(ctx)
	go a.runSlowLoop(ctx)
	go a.runHeartbeatLoop(ctx)
	a.sampler.WaitReady(3 * sampleInterval)
	a.pushOnce()

	reloadCh := reloadSignal()
	for {
		select {
		case <-ctx.Done():
			slog.Info("agent stopped")
			return
		case <-reloadCh:
			config, err := ingest.LoadConfig()
			if err != nil {
				slog.Warn("reload config", "err", err)
				continue
			}

			a.configMu.Lock()
			a.Config = config
			schedule = config.PushSchedule
			a.configMu.Unlock()

			a.refreshSampler()

			if err := a.startCron(schedule); err != nil {
				slog.Warn("reload push schedule", "schedule", schedule, "err", err)
				continue
			}
			slog.Info("config reloaded", "schedule", schedule)
		}
	}
}

func (a *Agent) refreshSampler() {
	config := a.currentConfig()
	a.HasZFS = zfs.Available()
	a.sampler = collector.NewSampler(collector.Options{
		HasZFS:       a.HasZFS,
		EnableDocker: config.EnableDocker,
		EnableGPU:    config.EnableGPU,
	})
}

func (a *Agent) runSampleLoop(ctx context.Context) {
	for {
		timer := time.NewTimer(sampleInterval)
		select {
		case <-ctx.Done():
			timer.Stop()
			return
		case <-timer.C:
			if a.sampler == nil {
				continue
			}
			if err := a.sampler.Tick(); err != nil {
				slog.Warn("sample tick", "err", err)
			}
		}
	}
}

func (a *Agent) runHeartbeatLoop(ctx context.Context) {
	ticker := time.NewTicker(heartbeatInterval)
	defer ticker.Stop()

	a.heartbeatOnce()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			a.heartbeatOnce()
		}
	}
}

func (a *Agent) heartbeatOnce() {
	config := a.currentConfig()
	if err := ingest.Heartbeat(config, a.Version); err != nil {
		slog.Warn("heartbeat", "err", err)
	}
}

func (a *Agent) runSlowLoop(ctx context.Context) {
	for {
		timer := time.NewTimer(slowMetricsInterval)
		select {
		case <-ctx.Done():
			timer.Stop()
			return
		case <-timer.C:
			if a.sampler == nil {
				continue
			}
			if err := a.sampler.RefreshSlow(); err != nil {
				slog.Warn("slow metrics refresh", "err", err)
			}
		}
	}
}

func (a *Agent) pushSchedule() string {
	a.configMu.RLock()
	defer a.configMu.RUnlock()
	return a.Config.PushSchedule
}

func (a *Agent) startCron(schedule string) error {
	if err := ingest.ValidatePushSchedule(schedule); err != nil {
		return err
	}

	c := cron.New(
		cron.WithSeconds(),
		cron.WithChain(cron.SkipIfStillRunning(cron.DiscardLogger)),
	)
	if _, err := c.AddFunc(schedule, a.pushOnce); err != nil {
		return err
	}

	a.cronMu.Lock()
	defer a.cronMu.Unlock()

	oldCron := a.cron
	c.Start()
	a.cron = c

	if oldCron != nil {
		stopCtx := oldCron.Stop()
		<-stopCtx.Done()
	}
	return nil
}

func (a *Agent) stopCron() {
	a.cronMu.Lock()
	defer a.cronMu.Unlock()

	if a.cron == nil {
		return
	}
	stopCtx := a.cron.Stop()
	<-stopCtx.Done()
	a.cron = nil
}

func (a *Agent) currentConfig() *Config {
	a.configMu.RLock()
	defer a.configMu.RUnlock()
	return a.Config
}

func (a *Agent) pushOnce() {
	config := a.currentConfig()
	collectStart := time.Now()

	if uptime, err := host.UptimeSeconds(); err != nil {
		slog.Warn("uptime unavailable", "err", err)
	} else {
		a.ServerDetails.UptimeSeconds = uptime
	}

	a.ServerDetails.Ip = host.GetIP()

	if a.sampler == nil {
		slog.Error("collect metrics", "err", "sampler not initialized")
		return
	}

	ready, clockMHz, sample := a.sampler.PushSnapshot()
	if !ready {
		slog.Warn("metrics not ready, skipping push")
		return
	}
	a.ServerDetails.CPUClockMhz = clockMHz

	if err := ingest.Push(config, buildIngestData(a.Version, a.ServerDetails, sample), a.Version); err != nil {
		slog.Error("push metrics", "err", err)
		return
	}

	slog.Info("metrics pushed", "duration", time.Since(collectStart).Round(time.Millisecond))
}

func (a *Agent) PrintOnce() {
	collectStart := time.Now()
	a.refreshSampler()
	if a.sampler == nil {
		return
	}
	if err := a.sampler.RunOnce(sampleInterval); err != nil {
		slog.Error("collect metrics", "err", err)
		return
	}

	if uptime, err := host.UptimeSeconds(); err != nil {
		slog.Warn("uptime unavailable", "err", err)
	} else {
		a.ServerDetails.UptimeSeconds = uptime
	}
	a.ServerDetails.Ip = host.GetIP()
	_, clockMHz, sample := a.sampler.PushSnapshot()
	a.ServerDetails.CPUClockMhz = clockMHz
	if err := ingest.Print(buildIngestData(a.Version, a.ServerDetails, sample)); err != nil {
		slog.Error("print metrics", "err", err)
		return
	}
	slog.Info("metrics printed", "duration", time.Since(collectStart).Round(time.Millisecond))
}

func buildIngestData(version string, details ingest.ServerDetails, sample collector.Result) ingest.Data {
	return ingest.Data{
		AgentVersion:         version,
		ServerDetails:        details,
		ServerMetrics:        sample.ServerMetrics,
		ZfsArcMetrics:        sample.ZfsArcMetrics,
		InterfaceMetrics:     sample.InterfaceMetrics,
		DiskMetrics:          sample.DiskMetrics,
		ZfsPoolMetrics:       sample.ZfsPoolMetrics,
		DockerContainers:     sample.DockerContainers,
		GPUMetrics:           sample.GPUMetrics,
		TCPConnectionMetrics: sample.TCPConnectionMetrics,
	}
}

func InitLogger() {
	level := ingest.ParseLogLevel(os.Getenv("MONITOR_LOG_LEVEL"))
	handler := slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{Level: level})
	slog.SetDefault(slog.New(handler))
}
