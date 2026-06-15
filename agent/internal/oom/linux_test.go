//go:build linux

package oom

import (
	"strings"
	"testing"
)

func TestReadVmstatOOMKills(t *testing.T) {
	t.Parallel()

	input := strings.NewReader("nr_free_pages 123\noom_kill 42\n")
	total, ok := parseVmstatOOMKills(input)
	if !ok || total != 42 {
		t.Fatalf("parseVmstatOOMKills() = (%d, %v), want (42, true)", total, ok)
	}
}
