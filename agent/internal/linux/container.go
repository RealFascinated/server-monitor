//go:build linux

package linux

import (
	"os"
	"os/exec"
	"strings"
	"sync"
)

var (
	containerOnce sync.Once
	containerEnv  bool
	lxcOnce       sync.Once
	lxcEnv        bool
	lxcfsOnce     sync.Once
	lxcfsActive   bool
)

func IsContainer() bool {
	containerOnce.Do(func() {
		containerEnv = detectContainer()
	})
	return containerEnv
}

func detectContainer() bool {
	if data, err := os.ReadFile("/proc/1/environ"); err == nil {
		if strings.Contains(string(data), "container=") {
			return true
		}
	}
	if data, err := os.ReadFile("/proc/self/cgroup"); err == nil {
		s := string(data)
		if strings.Contains(s, "/lxc/") || strings.Contains(s, ".lxc") {
			return true
		}
	}
	if out, err := exec.Command("systemd-detect-virt", "-c").Output(); err == nil {
		return strings.TrimSpace(string(out)) != "none"
	}
	return false
}

// IsLXC reports whether the agent runs inside an LXC container (not Docker or other runtimes).
func IsLXC() bool {
	lxcOnce.Do(func() {
		lxcEnv = detectLXC()
	})
	return lxcEnv
}

func detectLXC() bool {
	if data, err := os.ReadFile("/proc/1/environ"); err == nil {
		if environHasLXC(data) {
			return true
		}
	}
	if out, err := exec.Command("systemd-detect-virt", "-c").Output(); err == nil {
		if isLXCVirt(string(out)) {
			return true
		}
	}
	if data, err := os.ReadFile("/proc/self/cgroup"); err == nil {
		s := string(data)
		return strings.Contains(s, "/lxc/") || strings.Contains(s, ".lxc")
	}
	return false
}

// environHasLXC checks a NUL-separated /proc/<pid>/environ blob for container=lxc.
func environHasLXC(environ []byte) bool {
	for _, entry := range strings.Split(string(environ), "\x00") {
		if value, ok := strings.CutPrefix(entry, "container="); ok {
			return isLXCVirt(value)
		}
	}
	return false
}

// isLXCVirt matches the "lxc" and "lxc-libvirt" names used by systemd.
func isLXCVirt(name string) bool {
	return strings.HasPrefix(strings.TrimSpace(name), "lxc")
}

func LxcfsActive() bool {
	lxcfsOnce.Do(func() {
		data, err := os.ReadFile("/proc/self/mountinfo")
		if err != nil {
			return
		}
		s := string(data)
		lxcfsActive = strings.Contains(s, "lxcfs") && strings.Contains(s, "meminfo")
	})
	return lxcfsActive
}
