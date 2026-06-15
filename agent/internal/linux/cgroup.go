//go:build linux

package linux

import (
	"bufio"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

func Dir() string {
	if !IsContainer() {
		return ""
	}

	dir := "/sys/fs/cgroup"
	if cgroupDirUsable(dir) {
		return dir
	}
	if dir := cgroup1Dir(); dir != "" && cgroupDirUsable(dir) {
		return dir
	}
	return ""
}

func cgroupDirUsable(dir string) bool {
	for _, name := range []string{
		"cpu.stat",
		"memory.current",
		"cpuacct.usage",
		"cpuset.cpus.effective",
		"cpuset.cpus",
	} {
		if _, err := os.Stat(dir + "/" + name); err == nil {
			return true
		}
	}
	return false
}

func cgroup1Dir() string {
	data, err := os.ReadFile("/proc/self/cgroup")
	if err != nil {
		return ""
	}

	var controller, rel string
	for line := range strings.SplitSeq(strings.TrimSpace(string(data)), "\n") {
		fields := strings.SplitN(line, ":", 3)
		if len(fields) != 3 || fields[0] == "0" {
			continue
		}
		if strings.Contains(fields[1], "cpu") {
			controller = fields[1]
			rel = fields[2]
			break
		}
	}
	if rel == "" {
		for line := range strings.SplitSeq(strings.TrimSpace(string(data)), "\n") {
			fields := strings.SplitN(line, ":", 3)
			if len(fields) != 3 || fields[0] == "0" {
				continue
			}
			if strings.Contains(fields[1], "memory") {
				controller = fields[1]
				rel = fields[2]
				break
			}
		}
	}
	if rel == "" {
		return ""
	}

	mountRoot := cgroup1MountRoot(controller)
	if mountRoot == "" {
		return ""
	}
	return filepath.Join(mountRoot, strings.TrimPrefix(rel, "/"))
}

func cgroup1MountRoot(controller string) string {
	data, err := os.ReadFile("/proc/self/mountinfo")
	if err != nil {
		return ""
	}

	want := strings.Split(controller, ",")
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
		if sep < 0 || sep+1 >= len(fields) || fields[sep+1] != "cgroup" {
			continue
		}
		mountPoint := fields[4]
		opts := ""
		if sep+2 < len(fields) {
			opts = fields[sep+2]
		}
		for _, ctrl := range want {
			if strings.Contains(mountPoint, ctrl) || strings.Contains(opts, ctrl) {
				return mountPoint
			}
		}
	}
	return ""
}

// ParseCPUSet parses a cpuset.cpus list such as "0-3,8,10-11".
func ParseCPUSet(value string) map[string]struct{} {
	value = strings.TrimSpace(value)
	if value == "" {
		return nil
	}

	out := make(map[string]struct{})
	for part := range strings.SplitSeq(value, ",") {
		part = strings.TrimSpace(part)
		if part == "" {
			continue
		}
		if dash := strings.Index(part, "-"); dash >= 0 {
			start, err1 := strconv.Atoi(strings.TrimSpace(part[:dash]))
			end, err2 := strconv.Atoi(strings.TrimSpace(part[dash+1:]))
			if err1 != nil || err2 != nil {
				continue
			}
			if end < start {
				start, end = end, start
			}
			for n := start; n <= end; n++ {
				out[strconv.Itoa(n)] = struct{}{}
			}
			continue
		}
		out[part] = struct{}{}
	}
	if len(out) == 0 {
		return nil
	}
	return out
}

// EffectiveCPUs returns the CPUs assigned to the cgroup, if known.
func EffectiveCPUs(dir string) (map[string]struct{}, bool) {
	if dir == "" {
		return nil, false
	}
	for _, name := range []string{"cpuset.cpus.effective", "cpuset.cpus"} {
		data, err := os.ReadFile(dir + "/" + name)
		if err != nil {
			continue
		}
		cpus := ParseCPUSet(string(data))
		if len(cpus) > 0 {
			return cpus, true
		}
	}
	return nil, false
}

type CgroupMemory struct {
	Max, Current, File uint64
	OK                 bool
}

// Usage reports memory consumption using Proxmox's cgroup v2 formula:
// memory.current minus memory.stat file (page cache, tmpfs, and shmem).
func (m CgroupMemory) Usage() uint64 {
	if !m.OK || m.Current <= m.File {
		return 0
	}
	return m.Current - m.File
}

// Available is the cgroup memory limit minus Usage.
func (m CgroupMemory) Available() uint64 {
	if !m.OK {
		return 0
	}
	usage := m.Usage()
	if usage >= m.Max {
		return 0
	}
	return m.Max - usage
}

func ReadCgroupMemory(limitFallback uint64) CgroupMemory {
	searchDirs := cgroupMemorySearchDirs()

	var current, file uint64
	var haveUsage bool
	for _, dir := range searchDirs {
		if c, f, ok := readCgroupMemoryUsage(dir); ok {
			current, file = c, f
			haveUsage = true
			break
		}
	}
	if !haveUsage {
		return CgroupMemory{}
	}

	var max uint64
	for _, dir := range searchDirs {
		if m, ok := readCgroupMemoryMax(dir); ok {
			max = m
			break
		}
	}
	if max == 0 && limitFallback > 0 && (IsContainer() || LxcfsActive()) {
		max = limitFallback
	}
	if max == 0 {
		return CgroupMemory{}
	}

	return CgroupMemory{
		Max:     max,
		Current: current,
		File:    file,
		OK:      true,
	}
}

func readCgroupMemoryUsage(dir string) (current, file uint64, ok bool) {
	current, ok = readCgroupMemoryCurrent(dir)
	if !ok {
		return 0, 0, false
	}
	return current, readCgroupMemoryFile(dir), true
}

func cgroupV2Dir() string {
	data, err := os.ReadFile("/proc/self/cgroup")
	if err != nil {
		return cgroupMemoryBaseDir()
	}
	for line := range strings.SplitSeq(strings.TrimSpace(string(data)), "\n") {
		fields := strings.SplitN(line, ":", 3)
		if len(fields) != 3 || fields[0] != "0" {
			continue
		}
		rel := strings.TrimPrefix(fields[2], "/")
		if rel == "" {
			return cgroupMemoryBaseDir()
		}
		return filepath.Join("/sys/fs/cgroup", rel)
	}
	return cgroupMemoryBaseDir()
}

func cgroupMemorySearchDirs() []string {
	if LxcfsActive() || IsContainer() {
		return cgroupNamespaceRootDirs()
	}
	return cgroupProcessMemoryDirs()
}

func cgroupNamespaceRootDirs() []string {
	root := cgroupMemoryBaseDir()
	return uniqueDirs(root, root+"/.lxc", "/sys/fs/cgroup", "/sys/fs/cgroup/.lxc")
}

func cgroupProcessMemoryDirs() []string {
	var dirs []string
	for dir := cgroupV2Dir(); strings.HasPrefix(dir, "/sys/fs/cgroup"); dir = filepath.Dir(dir) {
		dirs = append(dirs, dir)
		if dir == "/sys/fs/cgroup" {
			break
		}
	}
	return uniqueDirs(dirs...)
}

func cgroupMemoryBaseDir() string {
	if d := Dir(); d != "" {
		return d
	}
	return "/sys/fs/cgroup"
}

func uniqueDirs(dirs ...string) []string {
	seen := make(map[string]struct{}, len(dirs))
	out := make([]string, 0, len(dirs))
	for _, dir := range dirs {
		if dir == "" {
			continue
		}
		if _, ok := seen[dir]; ok {
			continue
		}
		seen[dir] = struct{}{}
		out = append(out, dir)
	}
	return out
}

func readCgroupMemoryMax(dir string) (uint64, bool) {
	for _, name := range []string{"memory.max", "memory.limit_in_bytes"} {
		data, err := os.ReadFile(dir + "/" + name)
		if err != nil {
			continue
		}
		value := strings.TrimSpace(string(data))
		if value == "max" {
			return 0, false
		}
		max, err := strconv.ParseUint(value, 10, 64)
		if err != nil || max == 0 {
			continue
		}
		return max, true
	}
	return 0, false
}

func readCgroupMemoryCurrent(dir string) (uint64, bool) {
	for _, name := range []string{"memory.current", "memory.usage_in_bytes"} {
		data, err := os.ReadFile(dir + "/" + name)
		if err != nil {
			continue
		}
		current, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
		if err != nil {
			continue
		}
		return current, true
	}
	return 0, false
}

// ReadCgroupOOMKills returns the cumulative oom_kill count from memory.events.
func ReadCgroupOOMKills() (uint64, bool) {
	for _, dir := range cgroupMemorySearchDirs() {
		if total, ok := readCgroupMemoryEventsOOMKill(dir); ok {
			return total, true
		}
	}
	return 0, false
}

func readCgroupMemoryEventsOOMKill(dir string) (uint64, bool) {
	f, err := os.Open(dir + "/memory.events")
	if err != nil {
		return 0, false
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) != 2 || fields[0] != "oom_kill" {
			continue
		}
		total, err := strconv.ParseUint(fields[1], 10, 64)
		if err != nil {
			return 0, false
		}
		return total, true
	}
	return 0, false
}

func readCgroupMemoryFile(dir string) uint64 {
	f, err := os.Open(dir + "/memory.stat")
	if err != nil {
		return 0
	}
	defer f.Close()

	var file uint64
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) != 2 {
			continue
		}
		value, err := strconv.ParseUint(fields[1], 10, 64)
		if err != nil {
			continue
		}
		switch fields[0] {
		case "file":
			return value
		case "total_cache":
			file = value
		}
	}
	return file
}

func ReadIOStats() map[string]CgroupIOEntry {
	dir := Dir()
	if dir == "" {
		return map[string]CgroupIOEntry{}
	}

	file, err := os.Open(dir + "/io.stat")
	if err != nil {
		return map[string]CgroupIOEntry{}
	}
	defer file.Close()

	stats := make(map[string]CgroupIOEntry)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 0 {
			continue
		}
		entry := CgroupIOEntry{}
		for _, field := range fields[1:] {
			parts := strings.SplitN(field, "=", 2)
			if len(parts) != 2 {
				continue
			}
			switch parts[0] {
			case "rbytes":
				entry.Rbytes = ParseUint64(parts[1])
			case "wbytes":
				entry.Wbytes = ParseUint64(parts[1])
			}
		}
		stats[fields[0]] = entry
	}
	return stats
}

func ResolveBlockDeviceName(majmin string) string {
	path := "/sys/dev/block/" + majmin
	link, err := os.Readlink(path)
	if err != nil {
		return ""
	}
	if strings.Contains(link, "/") {
		parts := strings.Split(link, "/")
		return parts[len(parts)-1]
	}
	return link
}
