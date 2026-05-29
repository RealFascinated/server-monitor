package zfs

import "strings"

type PoolIO struct {
	Nread    uint64
	Nwritten uint64
}

type PoolIORates struct {
	ReadBytesPerSecond  int64
	WriteBytesPerSecond int64
	ReadIops            int64
	WriteIops           int64
}

func MountGetsPoolIO(source, mount string) bool {
	if mount == "/" {
		return true
	}
	pool := PoolName(source)
	return pool != "" && source == pool
}

func PoolName(source string) string {
	if i := len(source); i > 0 {
		if j := strings.IndexByte(source, '/'); j >= 0 {
			return source[:j]
		}
		return source
	}
	return ""
}
