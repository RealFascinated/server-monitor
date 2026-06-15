//go:build linux

package gpu

import (
	"os/exec"
	"strings"
	"sync"
)

var (
	lspciOnce  sync.Once
	lspciNames map[string]string
)

func pciDeviceName(slot string) string {
	if slot == "" {
		return ""
	}
	lspciOnce.Do(func() {
		lspciNames = loadLspciNames()
	})
	return lspciNames[strings.ToLower(slot)]
}

func loadLspciNames() map[string]string {
	if _, err := exec.LookPath("lspci"); err != nil {
		return map[string]string{}
	}
	out, err := exec.Command("lspci", "-Dnn").Output()
	if err != nil {
		return map[string]string{}
	}

	names := make(map[string]string)
	for line := range strings.SplitSeq(string(out), "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		slot, name := parseLspciLine(line)
		if slot != "" && name != "" {
			names[slot] = name
		}
	}
	return names
}

func parseLspciLine(line string) (slot, name string) {
	space := strings.Index(line, " ")
	if space < 0 {
		return "", ""
	}
	slot = strings.ToLower(line[:space])

	desc := line[space+1:]
	colonBracket := strings.Index(desc, "]: ")
	if colonBracket < 0 {
		return "", ""
	}
	rest := desc[colonBracket+3:]
	if i := strings.LastIndex(rest, " (rev "); i >= 0 {
		rest = strings.TrimSpace(rest[:i])
	}
	if i := strings.LastIndex(rest, " ["); i >= 0 {
		bracket := rest[i+2:]
		if strings.Contains(bracket, ":") && strings.HasSuffix(strings.TrimSpace(bracket), "]") {
			rest = strings.TrimSpace(rest[:i])
		}
	}
	return slot, rest
}
