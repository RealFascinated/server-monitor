package network

import (
	"runtime"
	"testing"
)

func TestNormalizeInterface(t *testing.T) {
	if got := NormalizeInterface("eth0@veth123"); got != "eth0" {
		t.Fatalf("unexpected normalize result: %q", got)
	}
}

func TestIsCommonInterface(t *testing.T) {
	tests := []struct {
		name string
		want bool
	}{
		{"eth0", true},
		{"lo", false},
	}
	if runtime.GOOS == "linux" {
		tests = append(tests,
			struct{ name string; want bool }{"veth123", false},
			struct{ name string; want bool }{"docker0", false},
		)
	}
	for _, tc := range tests {
		if got := IsCommonInterface(tc.name); got != tc.want {
			t.Fatalf("%s: expected %v, got %v", tc.name, tc.want, got)
		}
	}
}

func TestComputeMetricsScalesElapsed(t *testing.T) {
	before := []Counter{{
		Name:        "eth0",
		BytesRecv:   0,
		BytesSent:   0,
		PacketsRecv: 0,
		PacketsSent: 0,
	}}
	after := []Counter{{
		Name:        "eth0",
		BytesRecv:   2000,
		BytesSent:   1000,
		PacketsRecv: 20,
		PacketsSent: 10,
	}}

	metrics := ComputeMetrics(before, after, 2_000_000_000) // 2s
	if len(metrics) != 1 {
		t.Fatalf("expected 1 metric, got %d", len(metrics))
	}
	if metrics[0].RxBytesPerSecond != 1000 {
		t.Fatalf("expected 1000 B/s, got %d", metrics[0].RxBytesPerSecond)
	}
}
