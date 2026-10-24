package agent

import (
	"testing"

	"go.uber.org/goleak"
)

func TestMain(m *testing.M) {
	// gopsutil starts one load-average sampler on Windows and keeps it for the life of the process.
	goleak.VerifyTestMain(m,
		goleak.IgnoreAnyFunction("github.com/shirou/gopsutil/v4/load.loadAvgGoroutine"),
	)
}
