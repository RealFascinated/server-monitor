//go:build windows && !lhmbundle

package lhm

import "context"

// GetServerMetrics requires building with -tags lhmbundle after scripts/build-lhm.ps1.
func GetServerMetrics(ctx context.Context) (ServerSnapshot, error) {
	return ServerSnapshot{}, errUnavailable
}
