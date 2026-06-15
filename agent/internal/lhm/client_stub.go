//go:build !windows

package lhm

import "context"

// GetServerMetrics is only available on Windows.
func GetServerMetrics(ctx context.Context) (ServerSnapshot, error) {
	return ServerSnapshot{}, errUnavailable
}
