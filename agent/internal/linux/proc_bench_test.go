//go:build linux

package linux

import "testing"

func BenchmarkReadProcStat(b *testing.B) {
	b.ReportAllocs()
	for b.Loop() {
		_ = ReadProcStat()
	}
}

func BenchmarkReadDiskstats(b *testing.B) {
	b.ReportAllocs()
	for b.Loop() {
		_ = ReadDiskstats()
	}
}
