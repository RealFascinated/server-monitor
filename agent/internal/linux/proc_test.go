//go:build linux

package linux

import (
	"strings"
	"testing"
)

func TestParseProcStatInterrupts(t *testing.T) {
	t.Parallel()

	const sample = `cpu  30590826 206 11490982 143353050 502651 0 7755428 0 0 0
intr 13011360686 0 0 0 857198539 0 0 0 0 0 0 0 0 0 0 0 0 45811673
ctxt 19949740616
`
	snap := parseProcStat(strings.NewReader(sample))
	if snap.Interrupts != 13011360686 {
		t.Fatalf("Interrupts = %d, want 13011360686", snap.Interrupts)
	}
	if snap.ContextSwitches != 19949740616 {
		t.Fatalf("ContextSwitches = %d, want 19949740616", snap.ContextSwitches)
	}
}
