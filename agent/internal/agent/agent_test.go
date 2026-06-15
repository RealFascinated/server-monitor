package agent

import (
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"

	"fascinated.cc/monitor/agent/internal/collector"
	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestBuildIngestData(t *testing.T) {
	details := ingest.ServerDetails{Ip: "10.0.0.1", CoreCount: 4}
	sample := collector.Result{
		ServerMetrics:    ingest.ServerMetrics{CPUUsage: 42},
		InterfaceMetrics: []ingest.InterfaceMetrics{{InterfaceName: "eth0"}},
		DiskMetrics:      []ingest.DiskMetric{{DiskName: "sda"}},
	}

	data := buildIngestData("2.0.0", details, sample)
	if data.AgentVersion != "2.0.0" {
		t.Fatalf("AgentVersion = %q", data.AgentVersion)
	}
	if data.ServerDetails.Ip != "10.0.0.1" {
		t.Fatalf("ServerDetails.Ip = %q", data.ServerDetails.Ip)
	}
	if data.ServerMetrics.CPUUsage != 42 {
		t.Fatalf("CPUUsage = %v", data.ServerMetrics.CPUUsage)
	}
	if len(data.InterfaceMetrics) != 1 || data.InterfaceMetrics[0].InterfaceName != "eth0" {
		t.Fatalf("InterfaceMetrics = %+v", data.InterfaceMetrics)
	}
	if len(data.DiskMetrics) != 1 || data.DiskMetrics[0].DiskName != "sda" {
		t.Fatalf("DiskMetrics = %+v", data.DiskMetrics)
	}
}

func TestStartCronInvalidScheduleKeepsExisting(t *testing.T) {
	a := &Agent{
		Version: "2.0.0",
		Config: &Config{
			PushSchedule: "*/5 * * * * *",
		},
	}
	if err := a.startCron("*/5 * * * * *"); err != nil {
		t.Fatalf("startCron valid: %v", err)
	}
	defer a.stopCron()
	first := a.cron

	if err := a.startCron("not a cron"); err == nil {
		t.Fatal("expected error for invalid schedule")
	}
	if a.cron != first {
		t.Fatal("cron replaced after invalid schedule")
	}
}

func TestStartCronReplacesOnValidSchedule(t *testing.T) {
	a := &Agent{
		Version: "2.0.0",
		Config: &Config{
			PushSchedule: "*/5 * * * * *",
		},
	}
	if err := a.startCron("*/5 * * * * *"); err != nil {
		t.Fatalf("startCron: %v", err)
	}
	first := a.cron

	if err := a.startCron("*/10 * * * * *"); err != nil {
		t.Fatalf("startCron second: %v", err)
	}
	if a.cron == first {
		t.Fatal("cron not replaced after valid schedule change")
	}
	a.stopCron()
}

func TestPushOnceSkipsWhenNotReady(t *testing.T) {
	var pushes atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		pushes.Add(1)
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	a := &Agent{
		Version: "2.0.0",
		Config: &Config{
			IngestToken:  "test-token",
			ApiEndpoint:  srv.URL,
			PushSchedule: "*/5 * * * * *",
		},
		sampler: collector.NewSampler(collector.Options{}),
	}
	a.pushOnce()

	if pushes.Load() != 0 {
		t.Fatalf("expected 0 pushes when sampler not ready, got %d", pushes.Load())
	}
}
