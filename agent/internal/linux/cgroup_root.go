//go:build linux

package linux

import (
	"os"
	"path/filepath"
	"strings"
)

// CgroupRoot returns the host cgroup filesystem root.
func CgroupRoot() string {
	root := HostPath("/sys/fs/cgroup")
	if info, err := os.Stat(root); err != nil || !info.IsDir() {
		return ""
	}
	return root
}

// CgroupMountInfo returns cgroup v2 unified mount path when present.
func CgroupUnifiedMount() string {
	data, err := os.ReadFile(MountInfoPath())
	if err != nil {
		return ""
	}
	for line := range strings.SplitSeq(string(data), "\n") {
		fields := strings.Fields(line)
		if len(fields) < 5 {
			continue
		}
		sep := -1
		for i, field := range fields {
			if field == "-" {
				sep = i
				break
			}
		}
		if sep < 0 || sep+1 >= len(fields) {
			continue
		}
		if fields[sep+1] != "cgroup2" {
			continue
		}
		return fields[4]
	}
	return ""
}

// JoinCgroup joins path elements under the cgroup root.
func JoinCgroup(elem ...string) string {
	root := CgroupRoot()
	if root == "" {
		return ""
	}
	return filepath.Join(append([]string{root}, elem...)...)
}
