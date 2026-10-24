//go:build linux

package selfstats

import (
	"os"
	"testing"
)

func TestParseStatmRSS(t *testing.T) {
	tests := []struct {
		name string
		in   string
		want int64
	}{
		{"typical", "1000 250 100 10 0 300 0\n", 250 * 4096},
		{"too few fields", "1000", 0},
		{"not a number", "1000 abc 3", 0},
		{"negative", "1000 -5 3", 0},
		{"empty", "", 0},
	}
	for _, tc := range tests {
		if got := parseStatmRSS(tc.in, 4096); got != tc.want {
			t.Fatalf("%s: got %d, want %d", tc.name, got, tc.want)
		}
	}
}

func TestOpenFDsCountsNewDescriptors(t *testing.T) {
	before := openFDs()
	files := make([]*os.File, 0, 5)
	for range 5 {
		f, err := os.Open("/proc/self/statm")
		if err != nil {
			t.Fatal(err)
		}
		files = append(files, f)
	}
	defer func() {
		for _, f := range files {
			f.Close()
		}
	}()
	if got := openFDs(); got < before+5 {
		t.Fatalf("openFDs = %d, want at least %d", got, before+5)
	}
}
