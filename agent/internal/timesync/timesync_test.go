package timesync

import (
	"math"
	"testing"
)

func TestParseChronyTracking(t *testing.T) {
	tests := []struct {
		name   string
		out    string
		want   float64
		wantOK bool
	}{
		{
			name:   "clock slow",
			out:    "A29FC87B,162.159.200.1,3,1700000000.123456789,0.000123456,-0.000010,0.000020,5.1,0.0,0.1,0.02,0.003,64.0,Normal\n",
			want:   -0.123456,
			wantOK: true,
		},
		{
			name:   "clock fast",
			out:    "A29FC87B,162.159.200.1,2,1700000000.0,-0.002000000,0.0,0.0,0.0,0.0,0.0,0.0,0.0,64.0,Normal",
			want:   2,
			wantOK: true,
		},
		{name: "not synchronised", out: "00000000,,0,0.000000000,0.000000000,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,Not synchronised", wantOK: false},
		{name: "garbage", out: "506 Cannot talk to daemon", wantOK: false},
		{name: "bad number", out: "A29FC87B,1.2.3.4,3,1.0,abc,0,0", wantOK: false},
		{name: "empty", out: "", wantOK: false},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			got, ok := parseChronyTracking([]byte(tc.out))
			if ok != tc.wantOK || (ok && math.Abs(got-tc.want) > 1e-6) {
				t.Fatalf("got %v, %v; want %v, %v", got, ok, tc.want, tc.wantOK)
			}
		})
	}
}

const w32tmSynced = `Leap Indicator: 0(no warning)
Stratum: 4 (secondary reference - syncd by (S)NTP)
Precision: -23 (119.209ns per tick)
Last Successful Sync Time: 24/10/2026 20:11:03
Source: time.windows.com,0x8
Poll Interval: 10 (1024s)
Phase Offset: -0.0012345s
`

func TestParseW32tmStatus(t *testing.T) {
	synced, offset := parseW32tmStatus(w32tmSynced)
	if synced == nil || !*synced {
		t.Fatalf("synced = %v, want true", synced)
	}
	if offset == nil || math.Abs(*offset+1.2345) > 1e-9 {
		t.Fatalf("offset = %v, want -1.2345ms", offset)
	}
}

func TestParseW32tmStatusFreeRunning(t *testing.T) {
	synced, offset := parseW32tmStatus("Source: Local CMOS Clock\nPhase Offset: 0.0000000s\n")
	if synced == nil || *synced {
		t.Fatalf("synced = %v, want false for a local clock", synced)
	}
	if offset == nil || *offset != 0 {
		t.Fatalf("offset = %v, want 0", offset)
	}
}

func TestParseW32tmStatusUnknownOutput(t *testing.T) {
	synced, offset := parseW32tmStatus("The following error occurred: The service has not been started. (0x80070426)")
	if synced != nil || offset != nil {
		t.Fatalf("got %v, %v; want nothing for output we do not understand", synced, offset)
	}
}
