//go:build linux

package linux

import (
	"bufio"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

var (
	mdPartPattern = regexp.MustCompile(`^md[0-9]+p[0-9]+$`)
)

func ResolveDiskstatsDeviceName(source string, diskstats map[string]DiskstatsEntry) string {
	if !strings.HasPrefix(source, "/dev/") {
		return ""
	}

	path, err := filepath.EvalSymlinks(source)
	if err != nil {
		path = source
	}
	base := filepath.Base(path)

	hasDevice := func(name string) bool {
		if diskstats != nil {
			_, ok := diskstats[name]
			return ok
		}
		return diskstatsHasDevice(name)
	}

	if mdPartPattern.MatchString(base) {
		if parent := strings.SplitN(base, "p", 2)[0]; hasDevice(parent) {
			return parent
		}
	}

	for _, candidate := range []string{filepath.Base(source), base} {
		if candidate != "" && hasDevice(candidate) {
			return candidate
		}
	}

	if nvmePartPattern.MatchString(base) {
		if parent := nvmePartPattern.FindStringSubmatch(base); len(parent) == 2 && hasDevice(parent[1]) {
			return parent[1]
		}
	}
	if sdPartPattern.MatchString(base) {
		if parent := sdPartPattern.FindStringSubmatch(base); len(parent) == 2 && hasDevice(parent[1]) {
			return parent[1]
		}
	}
	return ""
}

func diskstatsHasDevice(name string) bool {
	file, err := os.Open("/proc/diskstats")
	if err != nil {
		return false
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) > 2 && fields[2] == name {
			return true
		}
	}
	return false
}
