//go:build !linux && !windows

package cpu

import (
	"fmt"
	"runtime"
)

func currentClockSpeedMHz() (float64, error) {
	return 0, fmt.Errorf("live cpu frequency not supported on %s", runtime.GOOS)
}
