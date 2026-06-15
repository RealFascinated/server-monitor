package zfs

import (
	"testing"
	"time"
)

func TestComputePoolIORates(t *testing.T) {
	before := map[string]PoolIO{
		"tank": {Nread: 1000, Nwritten: 2000, Reads: 10, Writes: 20},
	}
	after := map[string]PoolIO{
		"tank": {Nread: 2000, Nwritten: 4000, Reads: 20, Writes: 40},
	}

	rates := ComputePoolIORates(before, after, time.Second)
	r := rates["tank"]
	if r.ReadBytesPerSecond != 1000 || r.WriteBytesPerSecond != 2000 {
		t.Fatalf("byte rates: read=%d write=%d", r.ReadBytesPerSecond, r.WriteBytesPerSecond)
	}
	if r.ReadIops != 10 || r.WriteIops != 20 {
		t.Fatalf("iops: read=%d write=%d", r.ReadIops, r.WriteIops)
	}
}
