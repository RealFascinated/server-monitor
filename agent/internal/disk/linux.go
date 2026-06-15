//go:build linux

package disk

import (
	"bufio"
	"os"
	"regexp"
	"strings"
	"sync"
	"syscall"
	"time"

	"fascinated.cc/monitor/agent/internal/linux"
)

const mountCacheTTL = 30 * time.Second

var (
	mountCacheMu sync.Mutex
	mountCache   []Mount
	mountCacheAt time.Time
)

var (
	nfsSourcePattern     = regexp.MustCompile(`^[0-9]+(\.[0-9]+){3}:`)
	snapshotPattern      = regexp.MustCompile(`^[^/]+@`)
	linuxExcludedFSTypes = map[string]struct{}{
		"tmpfs": {}, "devtmpfs": {}, "overlay": {}, "squashfs": {}, "efivarfs": {},
		"fuse": {}, "fusectl": {}, "shfs": {}, "autofs": {}, "nsfs": {}, "binfmt_misc": {},
		"tracefs": {}, "debugfs": {}, "securityfs": {}, "pstore": {}, "hugetlbfs": {},
		"mqueue": {}, "configfs": {}, "rpc_pipefs": {}, "nfs": {}, "nfs4": {}, "cifs": {},
		"smb3": {}, "9p": {}, "ceph": {}, "cephfs": {}, "glusterfs": {}, "vmhgfs": {}, "vboxsf": {},
	}
)

type mountEntry struct {
	device     string
	mountpoint string
	fstype     string
}

func ListMounts() ([]Mount, error) {
	mountCacheMu.Lock()
	if !mountCacheAt.IsZero() && time.Since(mountCacheAt) < mountCacheTTL {
		cached := append([]Mount(nil), mountCache...)
		mountCacheMu.Unlock()
		return cached, nil
	}
	mountCacheMu.Unlock()

	entries, err := readMountEntries()
	if err != nil {
		return nil, err
	}

	mounts := make([]Mount, 0, len(entries))
	for _, entry := range entries {
		if !shouldIncludeLinuxMount(entry) {
			continue
		}

		used, total, inodeUsed, inodeTotal, err := statfsUsage(linux.HostPath(entry.mountpoint))
		if err != nil || total == 0 {
			continue
		}

		diskType := "block"
		if entry.fstype == "zfs" {
			diskType = "zfs"
		}

		mounts = append(mounts, Mount{
			Name:       entry.mountpoint,
			Source:     entry.device,
			Fstype:     entry.fstype,
			DiskType:   diskType,
			UsedBytes:  used,
			TotalBytes: total,
			InodeUsed:  inodeUsed,
			InodeTotal: inodeTotal,
		})
	}

	mounts = dedupeMountsByUsage(mounts)

	mountCacheMu.Lock()
	mountCache = append([]Mount(nil), mounts...)
	mountCacheAt = time.Now()
	mountCacheMu.Unlock()

	return mounts, nil
}

func readMountEntries() ([]mountEntry, error) {
	file, err := os.Open(linux.MountInfoPath())
	if err != nil {
		return nil, err
	}
	defer file.Close()

	entries := make([]mountEntry, 0, 32)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		dash := strings.Index(line, " - ")
		if dash < 0 {
			continue
		}

		before := strings.Fields(line[:dash])
		after := strings.Fields(line[dash+3:])
		if len(before) < 5 || len(after) < 2 {
			continue
		}

		entries = append(entries, mountEntry{
			device:     after[1],
			mountpoint: unescapeMountPath(before[4]),
			fstype:     after[0],
		})
	}
	return entries, scanner.Err()
}

func unescapeMountPath(path string) string {
	path = strings.ReplaceAll(path, "\\040", " ")
	path = strings.ReplaceAll(path, "\\011", "\t")
	path = strings.ReplaceAll(path, "\\012", "\n")
	path = strings.ReplaceAll(path, "\\134", "\\")
	return path
}

func statfsUsage(path string) (used, total, inodeUsed, inodeTotal uint64, err error) {
	var stat syscall.Statfs_t
	if err = syscall.Statfs(path, &stat); err != nil {
		return 0, 0, 0, 0, err
	}

	blockSize := uint64(stat.Bsize)
	total = uint64(stat.Blocks) * blockSize
	avail := uint64(stat.Bavail) * blockSize
	if total > avail {
		used = total - avail
	}

	inodeTotal = stat.Files
	if stat.Files >= stat.Ffree {
		inodeUsed = stat.Files - stat.Ffree
	}
	return used, total, inodeUsed, inodeTotal, nil
}

func shouldIncludeLinuxMount(entry mountEntry) bool {
	if _, excluded := linuxExcludedFSTypes[entry.fstype]; excluded {
		return false
	}
	if isExcludedLinuxMountpoint(entry.mountpoint) {
		return false
	}
	if isExcludedLinuxSource(entry.device) {
		return false
	}

	switch entry.fstype {
	case "zfs":
		return true
	default:
		return strings.HasPrefix(entry.device, "/")
	}
}

func isExcludedLinuxMountpoint(mountpoint string) bool {
	excluded := []string{
		"/run", "/dev", "/sys", "/proc", "/snap",
		"/var/lib/docker", "/var/lib/containerd", "/var/lib/lxc", "/var/lib/libvirt",
	}
	for _, prefix := range excluded {
		if mountpoint == prefix || strings.HasPrefix(mountpoint, prefix+"/") {
			return true
		}
	}
	return false
}

func isExcludedLinuxSource(source string) bool {
	if nfsSourcePattern.MatchString(source) {
		return true
	}
	if snapshotPattern.MatchString(source) {
		return true
	}
	if strings.HasPrefix(source, "/dev/zd") || strings.HasPrefix(source, "/dev/fuse") || strings.HasPrefix(source, "/dev/loop") {
		return true
	}
	excludedPrefixes := []string{"shfs", "mergerfs", "portal", "rclone", "vmhgfs", "vboxsf"}
	for _, prefix := range excludedPrefixes {
		if strings.HasPrefix(source, prefix) {
			return true
		}
	}
	return false
}

func resolveDiskDevice(source string, beforeIO, afterIO map[string]IOCounters) string {
	diskstats := make(map[string]linux.DiskstatsEntry, len(beforeIO)+len(afterIO))
	for name := range beforeIO {
		diskstats[name] = linux.DiskstatsEntry{}
	}
	for name := range afterIO {
		diskstats[name] = linux.DiskstatsEntry{}
	}
	return linux.ResolveDiskstatsDeviceName(source, diskstats)
}
