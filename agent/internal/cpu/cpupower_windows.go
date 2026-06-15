//go:build windows

package cpu

import (
	"sync"

	"fascinated.cc/monitor/agent/internal/winpdh"

	"golang.org/x/sys/windows"
)

// Windows reports RAPL package power in milliwatts via the Energy Meter counter set.
var cpuPowerCounterPaths = []string{
	`\Energy Meter(RAPL_Package0_PKG)\Power`,
	`\Energy Meter(_Total)\Power`,
}

type cpuPowerCounter struct {
	mu       sync.Mutex
	query    winpdh.Query
	counter  windows.Handle
	initOnce sync.Once
	initErr  error
}

var platformCPUPower cpuPowerCounter

// BeginCPUPowerSample starts a PDH interval for RAPL package power sampling.
func BeginCPUPowerSample() {
	platformCPUPower.begin()
}

// EndCPUPowerSample returns CPU package power in watts since BeginCPUPowerSample.
func EndCPUPowerSample() (float64, bool) {
	return platformCPUPower.end()
}

func (c *cpuPowerCounter) begin() {
	c.initOnce.Do(c.init)

	c.mu.Lock()
	defer c.mu.Unlock()
	if c.initErr != nil {
		return
	}
	c.query.Collect()
}

func (c *cpuPowerCounter) end() (float64, bool) {
	c.initOnce.Do(c.init)

	c.mu.Lock()
	defer c.mu.Unlock()
	if c.initErr != nil {
		return 0, false
	}

	c.query.Collect()

	milliwatts, err := winpdh.ReadDouble(c.counter)
	if err != nil || milliwatts <= 0 {
		return 0, false
	}

	watts := milliwatts / 1000
	if watts > 10_000 {
		return 0, false
	}
	return watts, true
}

func (c *cpuPowerCounter) init() {
	query, err := winpdh.OpenQuery()
	if err != nil {
		c.initErr = err
		return
	}

	var counter windows.Handle
	for _, path := range cpuPowerCounterPaths {
		counter, err = query.AddCounter(path)
		if err == nil {
			break
		}
	}
	if err != nil {
		query.Close()
		c.initErr = err
		return
	}

	c.query = query
	c.counter = counter
}
