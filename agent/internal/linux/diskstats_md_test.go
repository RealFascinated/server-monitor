//go:build linux

package linux

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestMdArrayName(t *testing.T) {
	t.Parallel()

	cases := map[string]string{
		"md3p1":  "md3",
		"md10p2": "md10",
		"md127":  "md127",
		"sdc1":   "",
	}
	for in, want := range cases {
		if got := mdArrayName(in); got != want {
			t.Fatalf("mdArrayName(%q) = %q, want %q", in, got, want)
		}
	}
}

func TestMdDeviceHasDiskstatsIO(t *testing.T) {
	t.Parallel()

	stats := map[string]DiskstatsEntry{
		"md0": {Reads: 10},
		"md3p1": {},
	}
	if !mdDeviceHasDiskstatsIO("md0", stats) {
		t.Fatal("expected md0 to report io")
	}
	if mdDeviceHasDiskstatsIO("md3p1", stats) {
		t.Fatal("expected md3p1 to have no io")
	}
}

func TestResolveMDSlaveDiskstatsNames(t *testing.T) {
	root := t.TempDir()
	mdDir := filepath.Join(root, "sys", "block", "md3", "slaves")
	if err := os.MkdirAll(mdDir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.Symlink("../../sdc", filepath.Join(mdDir, "sdc1")); err != nil {
		t.Fatal(err)
	}

	t.Setenv("MONITOR_HOST_ROOT", root)
	diskstats := map[string]DiskstatsEntry{
		"sdc":  {},
		"sdc1": {},
	}

	got := resolveMDSlaveDiskstatsNames("md3", diskstats)
	if len(got) != 1 || got[0] != "sdc" {
		t.Fatalf("resolveMDSlaveDiskstatsNames() = %v, want [sdc]", got)
	}

	got = resolveMDSlaveDiskstatsNames("md3", map[string]DiskstatsEntry{"sdc1": {}})
	if len(got) != 1 || got[0] != "sdc1" {
		t.Fatalf("partition-only diskstats = %v, want [sdc1]", got)
	}
}

func TestLookupDiskstatsDeltaMDUsesSlaves(t *testing.T) {
	root := t.TempDir()
	mdDir := filepath.Join(root, "sys", "block", "md3", "slaves")
	if err := os.MkdirAll(mdDir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.Symlink("../../sdc", filepath.Join(mdDir, "sdc1")); err != nil {
		t.Fatal(err)
	}
	t.Setenv("MONITOR_HOST_ROOT", root)

	before := map[string]DiskstatsEntry{
		"md3p1": {},
		"sdc": {
			Reads: 100, SectorsRead: 200, ReadMs: 10,
			Writes: 50, SectorsWritten: 80, WriteMs: 5, IoMs: 100,
		},
	}
	after := map[string]DiskstatsEntry{
		"md3p1": {},
		"sdc": {
			Reads: 200, SectorsRead: 600, ReadMs: 30,
			Writes: 100, SectorsWritten: 240, WriteMs: 15, IoMs: 300,
		},
	}

	rates, ok := LookupDiskstatsDelta("md3p1", before, after, time.Second)
	if !ok {
		t.Fatal("expected lookup to succeed via md slave fallback")
	}
	if rates.ReadBytesPerSecond == 0 || rates.WriteBytesPerSecond == 0 {
		t.Fatalf("expected non-zero throughput, got %+v", rates)
	}
	if rates.ReadIops == 0 || rates.WriteIops == 0 {
		t.Fatalf("expected non-zero iops, got %+v", rates)
	}
}

func TestLookupDiskstatsDeltaKeepsMDWhenIOPresent(t *testing.T) {
	t.Parallel()

	before := map[string]DiskstatsEntry{
		"md0": {Reads: 1000, SectorsRead: 8000, ReadMs: 100, IoMs: 1000},
	}
	after := map[string]DiskstatsEntry{
		"md0": {Reads: 1100, SectorsRead: 8800, ReadMs: 120, IoMs: 1200},
	}

	rates, ok := LookupDiskstatsDelta("md0", before, after, time.Second)
	if !ok {
		t.Fatal("expected lookup to succeed on md device")
	}
	if rates.ReadIops != 100 {
		t.Fatalf("ReadIops = %d, want 100", rates.ReadIops)
	}
}
