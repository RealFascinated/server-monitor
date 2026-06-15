//go:build linux

package connections

import (
	"strings"
	"testing"
)

func TestParseTCPTableCountsByState(t *testing.T) {
	t.Parallel()

	const sample = `sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
   0: 0100007F:0050 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 100 1 0000000000000000 20 4 30 10 10
   1: 0100007F:1234 0200007F:0050 01 00000000:00000000 00:00000000 00000000     0        0 101 1 0000000000000000 20 4 20 10 10
   2: 0100007F:2345 0300007F:0050 06 00000000:00000000 00:00000000 00000000     0        0 102 1 0000000000000000 20 4 20 10 10
   3: 0100007F:3456 0400007F:0050 08 00000000:00000000 00:00000000 00000000     0        0 103 1 0000000000000000 20 4 20 10 10
`
	counts := make(map[string]int64)
	parseTCPTable(strings.NewReader(sample), counts)

	if counts["LISTEN"] != 1 {
		t.Fatalf("LISTEN = %d, want 1", counts["LISTEN"])
	}
	if counts["ESTABLISHED"] != 1 {
		t.Fatalf("ESTABLISHED = %d, want 1", counts["ESTABLISHED"])
	}
	if counts["TIME_WAIT"] != 1 {
		t.Fatalf("TIME_WAIT = %d, want 1", counts["TIME_WAIT"])
	}
	if counts["CLOSE_WAIT"] != 1 {
		t.Fatalf("CLOSE_WAIT = %d, want 1", counts["CLOSE_WAIT"])
	}
}

func TestReadMergesIPv4AndIPv6(t *testing.T) {
	t.Parallel()

	const sample = `sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
   0: 0100007F:0050 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 100 1 0000000000000000 20 4 30 10 10
`
	counts := make(map[string]int64)
	parseTCPTable(strings.NewReader(sample), counts)
	parseTCPTable(strings.NewReader(sample), counts)

	if counts["LISTEN"] != 2 {
		t.Fatalf("LISTEN = %d, want 2 after merging two tables", counts["LISTEN"])
	}
}

func TestTrackerToIngestSorted(t *testing.T) {
	t.Parallel()

	metrics := Tracker{States: map[string]int64{
		"TIME_WAIT":   3,
		"ESTABLISHED": 10,
	}}.ToIngest()

	if len(metrics) != 2 {
		t.Fatalf("len = %d, want 2", len(metrics))
	}
	if metrics[0].State != "ESTABLISHED" || metrics[0].Count != 10 {
		t.Fatalf("first = %+v, want ESTABLISHED/10", metrics[0])
	}
	if metrics[1].State != "TIME_WAIT" || metrics[1].Count != 3 {
		t.Fatalf("second = %+v, want TIME_WAIT/3", metrics[1])
	}
}
