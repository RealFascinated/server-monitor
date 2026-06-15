//go:build linux

package linux

import (
	"os"
	"path/filepath"
	"strings"
)

const hostRootEnvVar = "MONITOR_HOST_ROOT"

// HostRoot returns MONITOR_HOST_ROOT when set to a host filesystem bind mount (e.g. /host).
func HostRoot() string {
	root := strings.TrimSpace(os.Getenv(hostRootEnvVar))
	root = strings.TrimSuffix(root, "/")
	if root == "" || root == "/" {
		return ""
	}
	return root
}

// HostPath maps an absolute host path into the container when MONITOR_HOST_ROOT is set.
func HostPath(path string) string {
	root := HostRoot()
	if root == "" || path == "" || !strings.HasPrefix(path, "/") {
		return path
	}
	return filepath.Join(root, path)
}

// MountInfoPath returns mountinfo for the host when MONITOR_HOST_ROOT is set.
func MountInfoPath() string {
	root := HostRoot()
	if root == "" {
		return "/proc/self/mountinfo"
	}
	return filepath.Join(root, "proc/1/mountinfo")
}
