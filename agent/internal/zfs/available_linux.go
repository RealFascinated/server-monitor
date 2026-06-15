//go:build linux

package zfs

import "os"

const arcStatsPath = "/proc/spl/kstat/zfs/arcstats"

func Available() bool {
	_, err := os.Stat(arcStatsPath)
	return err == nil
}
