//go:build linux

package zfs

import (
	"os"
	"path/filepath"
	"testing"
)

const samplePoolIOStats = `name                            type data
arc_read_count                  4    100
arc_read_bytes                  4    1000
arc_write_count                 4    200
arc_write_bytes                 4    2000
direct_read_count               4    10
direct_read_bytes               4    100
direct_write_count              4    20
direct_write_bytes              4    200
`

func TestPoolIOFromKstat(t *testing.T) {
	stats, err := readKstatStats(writeTempKstat(t, samplePoolIOStats))
	if err != nil {
		t.Fatalf("readKstatStats: %v", err)
	}
	io := poolIOFromKstat(stats)
	if io.Nread != 1100 || io.Nwritten != 2200 {
		t.Fatalf("bytes: read=%d write=%d", io.Nread, io.Nwritten)
	}
	if io.Reads != 110 || io.Writes != 220 {
		t.Fatalf("ops: read=%d write=%d", io.Reads, io.Writes)
	}
}

func writeTempKstat(t *testing.T, content string) string {
	t.Helper()
	dir := t.TempDir()
	path := filepath.Join(dir, "iostats")
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
	return path
}
