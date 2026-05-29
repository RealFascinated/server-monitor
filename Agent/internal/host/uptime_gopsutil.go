//go:build !linux

package host

import "github.com/shirou/gopsutil/v4/host"

func readUptime() (uint64, error) {
	return host.Uptime()
}
