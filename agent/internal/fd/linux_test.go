//go:build linux

package fd

import "testing"

func TestParseFileNr(t *testing.T) {
	t.Parallel()

	got := parseFileNr("30170\t0\t2097152\n")
	if got.Open != 30170 || got.Max != 2097152 {
		t.Fatalf("parseFileNr() = %#v, want open=30170 max=2097152", got)
	}
	if parseFileNr("bad").Open != 0 {
		t.Fatal("expected empty snapshot for bad input")
	}
}
