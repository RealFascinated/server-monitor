//go:build linux

package timesync

import (
	"syscall"

	"fascinated.cc/monitor/agent/internal/executil"
)

// staUnsync is the kernel's STA_UNSYNC status bit: set until a time daemon has synchronised the clock.
const staUnsync = 0x40

type kernelClock struct {
	status     int32
	esterrorUs int64
}

type kernelClockReader func() (kernelClock, bool)
type commandRunner func(name string, args ...string) ([]byte, error)

func read() Status {
	return readLinux(readKernelClock, runCommand)
}

func readKernelClock() (kernelClock, bool) {
	// Modes 0 only reads the state, so no privileges are needed. The kernel clock is not
	// namespaced, so this also works from inside a container.
	var timex syscall.Timex
	if _, err := syscall.Adjtimex(&timex); err != nil {
		return kernelClock{}, false
	}
	return kernelClock{status: timex.Status, esterrorUs: timex.Esterror}, true
}

func runCommand(name string, args ...string) ([]byte, error) {
	if _, err := executil.LookPath(name); err != nil {
		return nil, err
	}
	return executil.CommandOutput(name, args...)
}

func readLinux(kernel kernelClockReader, run commandRunner) Status {
	var status Status

	if clock, ok := kernel(); ok {
		synced := clock.status&staUnsync == 0
		status.Synced = boolPtr(synced)
		if synced {
			status.ErrorMs = floatPtr(float64(clock.esterrorUs) / 1000)
		}
	}

	// Only chrony reports a measured offset in a form that is cheap to read; with another daemon
	// the kernel values above are all there is.
	if out, err := run("chronyc", "-c", "tracking"); err == nil {
		if offset, ok := parseChronyTracking(out); ok {
			status.OffsetMs = floatPtr(offset)
		}
	}
	return status
}
