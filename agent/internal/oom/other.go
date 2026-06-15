//go:build !linux && !windows

package oom

func read() (uint64, bool) {
	return 0, false
}
