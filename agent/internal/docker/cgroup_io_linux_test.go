//go:build linux

package docker

import (
	"os"
	"path/filepath"
	"strconv"
	"testing"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

func TestReadCgroupBlockIOV2SumsDevices(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "io.stat"),
		"8:0 rbytes=1000 wbytes=200 rios=3 wios=4 dbytes=0 dios=0\n"+
			"259:0 rbytes=24 wbytes=800 rios=1 wios=2 dbytes=0 dios=0\n")

	read, write, ok := readCgroupBlockIO(dir)
	if !ok || read != 1024 || write != 1000 {
		t.Fatalf("got read=%d write=%d ok=%v, want 1024/1000/true", read, write, ok)
	}
}

func TestReadCgroupBlockIOV2IdleContainerHasEmptyFile(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "io.stat"), "")

	read, write, ok := readCgroupBlockIO(dir)
	if !ok || read != 0 || write != 0 {
		t.Fatalf("got read=%d write=%d ok=%v, want an available zero reading", read, write, ok)
	}
}

func TestReadCgroupBlockIOV1(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "blkio.throttle.io_service_bytes"),
		"8:0 Read 4096\n8:0 Write 8192\n8:0 Sync 0\n8:0 Async 12288\n8:0 Total 12288\n"+
			"8:16 Read 1\n8:16 Write 2\nTotal 12291\n")

	read, write, ok := readCgroupBlockIO(dir)
	if !ok || read != 4097 || write != 8194 {
		t.Fatalf("got read=%d write=%d ok=%v, want 4097/8194/true", read, write, ok)
	}
}

func TestReadCgroupBlockIOMissing(t *testing.T) {
	if _, _, ok := readCgroupBlockIO(t.TempDir()); ok {
		t.Fatal("expected no reading without io.stat or blkio files")
	}
}

const procNetDevFixture = `Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
    lo: 99999     100    0    0    0     0          0         0    99999     100    0    0    0     0       0          0
  eth0: 5000      10     0    0    0     0          0         0     3000      12    0    0    0     0       0          0
  eth1:  500       1     0    0    0     0          0         0      250       2    0    0    0     0       0          0
`

func TestReadProcNetDevSkipsLoopback(t *testing.T) {
	path := filepath.Join(t.TempDir(), "dev")
	writeFile(t, path, procNetDevFixture)

	rx, tx, ok := readProcNetDev(path)
	if !ok || rx != 5500 || tx != 3250 {
		t.Fatalf("got rx=%d tx=%d ok=%v, want 5500/3250/true", rx, tx, ok)
	}
}

func TestReadProcNetDevOnlyLoopbackIsNotAReading(t *testing.T) {
	path := filepath.Join(t.TempDir(), "dev")
	writeFile(t, path, "Inter-|   Receive\n face |bytes\n    lo: 1 1 0 0 0 0 0 0 1 1 0 0 0 0 0 0\n")

	if _, _, ok := readProcNetDev(path); ok {
		t.Fatal("a namespace with only loopback should report nothing")
	}
}

func TestFirstCgroupPIDSkipsPidsOutsideNamespace(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "cgroup.procs"), "0\n\nnot-a-pid\n4242\n4300\n")

	if got := firstCgroupPID(dir); got != "4242" {
		t.Fatalf("pid = %q, want 4242", got)
	}
	if got := firstCgroupPID(t.TempDir()); got != "" {
		t.Fatalf("pid = %q, want none for a missing cgroup.procs", got)
	}
}

func TestReadContainerNetworkSkipsContainersWithoutVisiblePid(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "cgroup.procs"), "0\n")

	if _, _, ok := readContainerNetwork(dir); ok {
		t.Fatal("expected no network reading when no process is visible")
	}
}

func TestReadContainerNetworkSkipsAgentsOwnNamespace(t *testing.T) {
	// The test process itself lives in the agent's namespace, which is exactly what a
	// host-network container looks like from the agent.
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, "cgroup.procs"), strconv.Itoa(os.Getpid())+"\n")

	if _, _, ok := readContainerNetwork(dir); ok {
		t.Fatal("a container in the agent's own network namespace must not report host traffic")
	}
}

func TestCounterRate(t *testing.T) {
	if rate := counterRate(1000, 3000, true, 2); rate == nil || *rate != 1000 {
		t.Fatalf("rate = %v, want 1000", rate)
	}
	if rate := counterRate(1000, 1000, true, 5); rate == nil || *rate != 0 {
		t.Fatalf("idle rate = %v, want an explicit 0", rate)
	}
	if rate := counterRate(0, 10, false, 1); rate != nil {
		t.Fatalf("first reading rate = %v, want none", rate)
	}
	if rate := counterRate(5000, 100, true, 1); rate != nil {
		t.Fatalf("counter reset rate = %v, want none", rate)
	}
	if rate := counterRate(1, 2, true, 0); rate != nil {
		t.Fatalf("zero elapsed rate = %v, want none", rate)
	}
}

func TestApplyIORatesRequiresReadingsOnBothSides(t *testing.T) {
	start := time.Unix(1_700_000_000, 0)
	prev := cgroupCPUState{at: start, io: ioCounters{hasBlock: true, blkReadBytes: 1000, blkWriteBytes: 0}}
	current := ioCounters{hasBlock: true, blkReadBytes: 3000, blkWriteBytes: 500, hasNet: true, rxBytes: 9}

	var metric ingest.DockerContainerMetric
	applyIORates(&metric, prev, current, start.Add(2*time.Second))

	if metric.BlockReadBytesPerSecond == nil || *metric.BlockReadBytesPerSecond != 1000 {
		t.Fatalf("block read = %v, want 1000", metric.BlockReadBytesPerSecond)
	}
	if metric.BlockWriteBytesPerSecond == nil || *metric.BlockWriteBytesPerSecond != 250 {
		t.Fatalf("block write = %v, want 250", metric.BlockWriteBytesPerSecond)
	}
	if metric.NetworkRxBytesPerSecond != nil || metric.NetworkTxBytesPerSecond != nil {
		t.Fatal("network rates need a previous network reading")
	}
}

func TestCollectFromCgroupsReportsBlockIORates(t *testing.T) {
	root := t.TempDir()
	scope := filepath.Join(root, "sys", "fs", "cgroup", "system.slice", "docker-abc123def456.scope")
	writeFile(t, filepath.Join(scope, "cpu.stat"), "usage_usec 1000000\n")
	writeFile(t, filepath.Join(scope, "memory.current"), "1048576\n")
	writeFile(t, filepath.Join(scope, "io.stat"), "8:0 rbytes=1000 wbytes=1000\n")
	t.Setenv("MONITOR_HOST_ROOT", root)
	resetCgroupCollector()

	first := collectFromCgroups()
	if first[0].BlockReadBytesPerSecond != nil {
		t.Fatalf("first sample block read = %v, want none", first[0].BlockReadBytesPerSecond)
	}

	writeFile(t, filepath.Join(scope, "io.stat"), "8:0 rbytes=1000000 wbytes=1000\n")
	time.Sleep(20 * time.Millisecond)
	second := collectFromCgroups()
	if second[0].BlockReadBytesPerSecond == nil || *second[0].BlockReadBytesPerSecond <= 0 {
		t.Fatalf("block read = %v, want a positive rate", second[0].BlockReadBytesPerSecond)
	}
	if second[0].BlockWriteBytesPerSecond == nil || *second[0].BlockWriteBytesPerSecond != 0 {
		t.Fatalf("block write = %v, want an explicit 0", second[0].BlockWriteBytesPerSecond)
	}
	if second[0].NetworkRxBytesPerSecond != nil {
		t.Fatal("no process in the fixture cgroup, so no network rates")
	}
}
