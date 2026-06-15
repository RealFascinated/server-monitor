//go:build linux

package docker

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strconv"
	"testing"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestCollectFromCgroupsCPUUsage(t *testing.T) {
	root := t.TempDir()
	cgroupRoot := filepath.Join(root, "sys", "fs", "cgroup")
	scope := filepath.Join(cgroupRoot, "system.slice", "docker-abc123def456.scope")
	if err := os.MkdirAll(scope, 0o755); err != nil {
		t.Fatal(err)
	}
	writeCPUStat := func(usec uint64) {
		t.Helper()
		if err := os.WriteFile(filepath.Join(scope, "cpu.stat"), []byte("usage_usec "+strconv.FormatUint(usec, 10)+"\n"), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	if err := os.WriteFile(filepath.Join(scope, "memory.current"), []byte("1048576\n"), 0o644); err != nil {
		t.Fatal(err)
	}

	t.Setenv("MONITOR_HOST_ROOT", root)
	resetCgroupCollector()

	writeCPUStat(1_000_000)
	metrics := collectFromCgroups()
	if len(metrics) != 1 {
		t.Fatalf("metrics = %d, want 1", len(metrics))
	}
	if metrics[0].CPUUsage != nil {
		t.Fatalf("first tick CPUUsage = %v, want omitted", metrics[0].CPUUsage)
	}

	writeCPUStat(1_000_000)
	metrics = collectFromCgroups()
	if metrics[0].CPUUsage == nil || *metrics[0].CPUUsage != 0 {
		t.Fatalf("idle CPUUsage = %v, want 0", metrics[0].CPUUsage)
	}

	writeCPUStat(2_000_000)
	time.Sleep(5 * time.Millisecond)
	metrics = collectFromCgroups()
	if metrics[0].CPUUsage == nil || *metrics[0].CPUUsage <= 0 {
		t.Fatalf("active CPUUsage = %v, want > 0", metrics[0].CPUUsage)
	}

	writeCPUStat(10_000)
	metrics = collectFromCgroups()
	if metrics[0].CPUUsage != nil {
		t.Fatalf("restart CPUUsage = %v, want omitted", metrics[0].CPUUsage)
	}

	data, err := json.Marshal(ingest.DockerContainerMetric{ContainerName: "x", CPUUsage: nil})
	if err != nil {
		t.Fatal(err)
	}
	if string(data) != `{"containerName":"x","memoryUsage":0}` {
		t.Fatalf("omit json = %s", data)
	}
	zero := 0.0
	data, err = json.Marshal(ingest.DockerContainerMetric{ContainerName: "x", CPUUsage: &zero})
	if err != nil {
		t.Fatal(err)
	}
	if string(data) != `{"containerName":"x","cpuUsage":0,"memoryUsage":0}` {
		t.Fatalf("zero json = %s", data)
	}
}

func resetCgroupCollector() {
	cgroupCollector.mu.Lock()
	cgroupCollector.prev = nil
	cgroupCollector.prevAt = time.Time{}
	cgroupCollector.mu.Unlock()
}

func TestDiscoverDockerCgroupsUnified(t *testing.T) {
	root := t.TempDir()
	cgroupRoot := filepath.Join(root, "sys", "fs", "cgroup")
	systemSlice := filepath.Join(cgroupRoot, "system.slice", "docker-abc123def456.scope")
	if err := os.MkdirAll(systemSlice, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(systemSlice, "cpu.stat"), []byte("usage_usec 1000000\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(systemSlice, "memory.current"), []byte("1048576\n"), 0o644); err != nil {
		t.Fatal(err)
	}

	t.Setenv("MONITOR_HOST_ROOT", root)

	containers := discoverDockerCgroups()
	if len(containers) != 1 {
		t.Fatalf("containers = %d, want 1", len(containers))
	}
	if containers[0].id != "abc123def456" {
		t.Fatalf("id = %q", containers[0].id)
	}
}

func BenchmarkDockerCgroupDiscovery(b *testing.B) {
	b.ReportAllocs()
	for b.Loop() {
		_ = discoverDockerCgroups()
	}
}
