//go:build windows

package cpu

import (
	"sync"

	"fascinated.cc/monitor/agent/internal/winpdh"

	"golang.org/x/sys/windows"
)

type iowaitCounter struct {
	mu       sync.Mutex
	query    winpdh.Query
	counter  windows.Handle
	initOnce sync.Once
	initErr  error
}

var platformIowait iowaitCounter

// BeginIowaitSample starts a PDH interval for Windows disk-busy sampling.
func BeginIowaitSample() {
	platformIowait.begin()
}

// EndIowaitSample returns physical disk busy percent since BeginIowaitSample.
// This is a Windows-specific proxy stored in CPUIowaitPercent for dashboard compatibility.
func EndIowaitSample() float64 {
	return platformIowait.end()
}

func (c *iowaitCounter) begin() {
	c.initOnce.Do(c.init)

	c.mu.Lock()
	defer c.mu.Unlock()
	if c.initErr != nil {
		return
	}
	c.query.Collect()
}

func (c *iowaitCounter) end() float64 {
	c.initOnce.Do(c.init)

	c.mu.Lock()
	defer c.mu.Unlock()
	if c.initErr != nil {
		return 0
	}

	c.query.Collect()

	value, err := winpdh.ReadDouble(c.counter)
	if err != nil {
		return 0
	}
	return clampPercent(value)
}

func (c *iowaitCounter) init() {
	query, err := winpdh.OpenQuery()
	if err != nil {
		c.initErr = err
		return
	}

	counter, err := query.AddCounter(`\PhysicalDisk(_Total)\% Disk Time`)
	if err != nil {
		query.Close()
		c.initErr = err
		return
	}

	c.query = query
	c.counter = counter

	query.Collect()
	query.Collect()
}
