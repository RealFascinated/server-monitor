package agent

import (
	"context"
	"log/slog"
	"os"
	"sync"
	"time"

	"github.com/robfig/cron/v3"
	"fascinated.cc/monitor/agent/internal/collector"
	"fascinated.cc/monitor/agent/internal/host"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/metric"
	"fascinated.cc/monitor/agent/internal/zfs"
)

const DefaultVersion = "2.0.0"

type Agent struct {
	Version       string
	Config        *Config
	ServerDetails ingest.ServerDetails
	HasZFS        bool

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
	schedule := a.pushSchedule()
	if err := a.startCron(schedule); err != nil {
		slog.Error("start push schedule", "schedule", schedule, "err", err)
		return
	}
	defer a.stopCron()

	slog.Info("agent started", "schedule", schedule)
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

			if err := a.startCron(schedule); err != nil {
				slog.Warn("reload push schedule", "schedule", schedule, "err", err)
				continue
			}
			slog.Info("config reloaded", "schedule", schedule)
		}
	}
}

func (a *Agent) pushSchedule() string {
	a.configMu.RLock()
	defer a.configMu.RUnlock()
	return a.Config.PushSchedule
}

func (a *Agent) startCron(schedule string) error {
	a.cronMu.Lock()
	defer a.cronMu.Unlock()

	if a.cron != nil {
		stopCtx := a.cron.Stop()
		<-stopCtx.Done()
	}

	c := cron.New(cron.WithSeconds())
	if _, err := c.AddFunc(schedule, a.pushOnce); err != nil {
		return err
	}
	c.Start()
	a.cron = c
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

	a.HasZFS = zfs.Available()

	if uptime, err := host.UptimeSeconds(); err != nil {
		slog.Warn("uptime unavailable", "err", err)
	} else {
		a.ServerDetails.UptimeSeconds = uptime
	}

	if mhz, err := metric.GetClockSpeedMHz(); err != nil {
		slog.Warn("cpu clock speed unavailable", "err", err)
	} else {
		a.ServerDetails.CPUClockMhz = mhz
	}

	a.ServerDetails.Ip = host.GetIP()

	sample, err := collector.Collect(collector.Options{
		HasZFS:       a.HasZFS,
		EnableDocker: config.EnableDocker,
	})
	if err != nil {
		slog.Error("collect metrics", "err", err)
		return
	}

	data := ingest.Data{
		AgentVersion:     a.Version,
		ServerDetails:    a.ServerDetails,
		ServerMetrics:    sample.ServerMetrics,
		ZfsArcMetrics:    sample.ZfsArcMetrics,
		InterfaceMetrics: sample.InterfaceMetrics,
		DiskMetrics:      sample.DiskMetrics,
		ZfsPoolMetrics:   sample.ZfsPoolMetrics,
		DockerContainers: sample.DockerContainers,
	}

	if err := ingest.Push(config, data, a.Version); err != nil {
		slog.Error("push metrics", "err", err)
		return
	}

	slog.Info("metrics pushed", "duration", time.Since(collectStart).Round(time.Millisecond))
}

func InitLogger() {
	level := ingest.ParseLogLevel(os.Getenv("MONITOR_LOG_LEVEL"))
	handler := slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{Level: level})
	slog.SetDefault(slog.New(handler))
}
