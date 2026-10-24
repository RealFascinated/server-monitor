//go:build linux

package selfstats

import (
	"os"
	"strconv"
	"strings"
)

func rssBytes() int64 {
	data, err := os.ReadFile("/proc/self/statm")
	if err != nil {
		return 0
	}
	return parseStatmRSS(string(data), int64(os.Getpagesize()))
}

// parseStatmRSS reads the resident page count (second field) from /proc/self/statm.
func parseStatmRSS(statm string, pageSize int64) int64 {
	fields := strings.Fields(statm)
	if len(fields) < 2 {
		return 0
	}
	pages, err := strconv.ParseInt(fields[1], 10, 64)
	if err != nil || pages < 0 {
		return 0
	}
	return pages * pageSize
}

func openFDs() int64 {
	entries, err := os.ReadDir("/proc/self/fd")
	if err != nil {
		return 0
	}
	// ReadDir holds one descriptor open on the directory itself while listing.
	if count := len(entries) - 1; count > 0 {
		return int64(count)
	}
	return 0
}
