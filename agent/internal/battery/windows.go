//go:build windows

package battery

import "github.com/yusufpapurcu/wmi"

type win32Battery struct {
	EstimatedChargeRemaining uint16
}

func read() Snapshot {
	var batteries []win32Battery
	if err := wmi.Query("SELECT EstimatedChargeRemaining FROM Win32_Battery", &batteries); err != nil || len(batteries) == 0 {
		return Snapshot{}
	}

	var sum float64
	for _, battery := range batteries {
		sum += float64(battery.EstimatedChargeRemaining)
	}
	return Snapshot{Percent: sum / float64(len(batteries)), OK: true}
}
