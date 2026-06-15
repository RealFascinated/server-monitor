//go:build !linux

package zfs

func ReadPoolIOSnapshots() map[string]PoolIO {
	return map[string]PoolIO{}
}
