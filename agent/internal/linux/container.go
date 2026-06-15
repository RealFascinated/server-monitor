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
