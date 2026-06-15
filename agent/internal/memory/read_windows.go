//go:build windows

package memory

import "github.com/shirou/gopsutil/v4/mem"

func Read() Snapshot {
	vm, err := mem.VirtualMemory()
	if err != nil {
		return Snapshot{}
	}
	return Snapshot{
		Usage:     float64(vm.Used),
		Available: float64(vm.Available),
		Total:     float64(vm.Total),
		Extras: Extras{
			Buffers:   int64(vm.Buffers),
			Cached:    int64(vm.Cached),
			SwapTotal: int64(vm.SwapTotal),
			SwapUsed:  int64(vm.SwapTotal - vm.SwapFree),
		},
	}
}
