//go:build linux

package reboot

import (
	"os"

	"fascinated.cc/monitor/agent/internal/linux"
)

// Required reports whether the host has flagged a pending reboot. Debian and Ubuntu create
// /run/reboot-required (with /var/run a symlink to it) when a package needs one. The path is
// mapped through MONITOR_HOST_ROOT, so a container sees the host's flag rather than its own.
func Required() bool {
	for _, path := range []string{"/var/run/reboot-required", "/run/reboot-required"} {
		if _, err := os.Stat(linux.HostPath(path)); err == nil {
			return true
		}
	}
	return false
}
