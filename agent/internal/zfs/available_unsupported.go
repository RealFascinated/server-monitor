//go:build !linux

package zfs

import "errors"

var errUnavailable = errors.New("zfs unavailable")

func Available() bool {
	return false
}

func readArcStats() (map[string]uint64, error) {
	return nil, errUnavailable
}
