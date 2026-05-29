//go:build linux

package linux

import (
	"os"
	"os/exec"
	"strings"
)

func IsContainer() bool {
	if data, err := os.ReadFile("/proc/1/environ"); err == nil {
		if strings.Contains(string(data), "container=") {
			return true
		}
	}
	if out, err := exec.Command("systemd-detect-virt", "-c").Output(); err == nil {
		return strings.TrimSpace(string(out)) != "none"
	}
	return false
}
