//go:build linux

package connections

import "testing"

func BenchmarkReadTCP(b *testing.B) {
	b.ReportAllocs()
	for b.Loop() {
		_ = Read()
	}
}

func BenchmarkCollectTCP(b *testing.B) {
	b.ReportAllocs()
	for b.Loop() {
		_ = CollectTCP()
	}
}
