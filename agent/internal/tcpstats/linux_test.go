//go:build linux

package tcpstats

import (
	"strings"
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

const procNetSnmp = `Ip: Forwarding DefaultTTL InReceives
Ip: 1 64 1000
Tcp: RtoAlgorithm RtoMin RtoMax MaxConn ActiveOpens PassiveOpens AttemptFails EstabResets CurrEstab InSegs OutSegs RetransSegs InErrs OutRsts InCsumErrors
Tcp: 1 200 120000 -1 100 200 1 2 3 1000 2000 42 1 1 0
`

const procNetNetstat = `TcpExt: SyncookiesSent SyncookiesRecv ListenOverflows ListenDrops
TcpExt: 0 0 7 9
`

func TestParseNetColumnsSNMP(t *testing.T) {
	columns, ok := parseNetColumns(strings.NewReader(procNetSnmp), "Tcp", "RetransSegs")
	if !ok {
		t.Fatal("expected Tcp columns")
	}
	if got := columns["RetransSegs"]; got != 42 {
		t.Fatalf("RetransSegs = %d, want 42", got)
	}
}

func TestParseNetColumnsNetstat(t *testing.T) {
	columns, ok := parseNetColumns(strings.NewReader(procNetNetstat), "TcpExt", "ListenOverflows", "ListenDrops")
	if !ok {
		t.Fatal("expected TcpExt columns")
	}
	if columns["ListenOverflows"] != 7 || columns["ListenDrops"] != 9 {
		t.Fatalf("listen columns = %+v, want 7/9", columns)
	}
}

func TestParseNetColumnsMissing(t *testing.T) {
	if _, ok := parseNetColumns(strings.NewReader(procNetSnmp), "TcpExt", "ListenDrops"); ok {
		t.Fatal("expected no TcpExt table")
	}
	if _, ok := parseNetColumns(strings.NewReader(procNetSnmp), "Tcp", "ListenDrops"); ok {
		t.Fatal("expected no matching column")
	}
}

func TestApplyRatesAndGauges(t *testing.T) {
	prev := Snapshot{
		RetransSegs:     uint64Ptr(100),
		ListenOverflows: uint64Ptr(10),
		ListenDrops:     uint64Ptr(10),
	}
	curr := Snapshot{
		RetransSegs:           uint64Ptr(140),
		ListenOverflows:       uint64Ptr(14),
		ListenDrops:           uint64Ptr(6), // counter reset
		ConntrackEntries:      int64Ptr(512),
		ConntrackMax:          int64Ptr(4096),
		ConntrackUsagePercent: float64Ptr(12.5),
	}

	var metrics ingest.ServerMetrics
	Apply(&metrics, prev, curr, true, 2e9) // 2s

	if metrics.TCPRetransSegmentsPerSecond == nil || *metrics.TCPRetransSegmentsPerSecond != 20 {
		t.Fatalf("retrans rate = %v, want 20", metrics.TCPRetransSegmentsPerSecond)
	}
	if metrics.TCPListenOverflowsPerSecond == nil || *metrics.TCPListenOverflowsPerSecond != 2 {
		t.Fatalf("overflows rate = %v, want 2", metrics.TCPListenOverflowsPerSecond)
	}
	if metrics.TCPListenDropsPerSecond == nil || *metrics.TCPListenDropsPerSecond != 3 {
		t.Fatalf("drops rate = %v, want 3 (reset from 10 to 6)", metrics.TCPListenDropsPerSecond)
	}
	if metrics.ConntrackEntries == nil || *metrics.ConntrackEntries != 512 {
		t.Fatalf("entries = %v", metrics.ConntrackEntries)
	}
	if metrics.ConntrackMax == nil || *metrics.ConntrackMax != 4096 {
		t.Fatalf("max = %v", metrics.ConntrackMax)
	}
	if metrics.ConntrackUsagePercent == nil || *metrics.ConntrackUsagePercent != 12.5 {
		t.Fatalf("usage = %v", metrics.ConntrackUsagePercent)
	}
}

func TestApplyFirstSampleOmitsRates(t *testing.T) {
	var metrics ingest.ServerMetrics
	Apply(&metrics, Snapshot{}, Snapshot{RetransSegs: uint64Ptr(140)}, false, 2e9)
	if metrics.TCPRetransSegmentsPerSecond != nil {
		t.Fatalf("rate = %v, want nil", metrics.TCPRetransSegmentsPerSecond)
	}
}
