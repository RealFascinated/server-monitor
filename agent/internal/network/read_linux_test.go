//go:build linux

package network

import (
	"strings"
	"testing"
)

const procNetDev = `Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
    lo:    1000      10    0    0    0     0          0         0     1000      10    0    0    0     0       0          0
  eth0:  500000    4000    1    2    0     0          0         0   600000    5000    3    4    0     0       0          0
`

func TestParseProcNetDev(t *testing.T) {
	counters, err := parseProcNetDev(strings.NewReader(procNetDev))
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if len(counters) != 2 {
		t.Fatalf("expected 2 counters, got %d", len(counters))
	}

	eth0 := counters[1]
	if eth0.Name != "eth0" {
		t.Fatalf("name = %q", eth0.Name)
	}
	if eth0.BytesRecv != 500000 || eth0.BytesSent != 600000 {
		t.Fatalf("bytes = %d/%d", eth0.BytesRecv, eth0.BytesSent)
	}
	if eth0.Errin != 1 || eth0.Errout != 3 {
		t.Fatalf("errors = %d/%d", eth0.Errin, eth0.Errout)
	}
	if eth0.Dropin != 2 || eth0.Dropout != 4 {
		t.Fatalf("drops = %d/%d", eth0.Dropin, eth0.Dropout)
	}
}

func TestParseProcNetDevSkipsShortRows(t *testing.T) {
	input := `Inter-|   Receive |  Transmit
 face |bytes packets errs drop fifo frame compressed multicast|bytes packets errs drop fifo colls carrier compressed
  eth0: 1 2 3
`
	if counters, err := parseProcNetDev(strings.NewReader(input)); err != nil || len(counters) != 0 {
		t.Fatalf("expected no counters, got %+v (err %v)", counters, err)
	}
}
