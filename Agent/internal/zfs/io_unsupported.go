//go:build !linux

package zfs

func ReadPoolIOSnapshots() map[string]PoolIO {
	return map[string]PoolIO{}
}

func StartPoolIostatSample() func() map[string]PoolIORates {
	return func() map[string]PoolIORates {
		return map[string]PoolIORates{}
	}
}
