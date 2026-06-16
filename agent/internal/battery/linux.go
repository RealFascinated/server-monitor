//go:build linux

package battery

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

func read() Snapshot {
	root := linux.HostPath("/sys/class/power_supply")
	entries, err := os.ReadDir(root)
	if err != nil {
		return Snapshot{}
	}

	var sum float64
	var count int
	for _, entry := range entries {
		dir := filepath.Join(root, entry.Name())
		typ, err := os.ReadFile(filepath.Join(dir, "type"))
		if err != nil || strings.TrimSpace(string(typ)) != "Battery" {
			continue
		}
		pct, ok := readCapacityPercent(dir)
		if !ok {
			continue
		}
		sum += pct
		count++
	}
	if count == 0 {
		return Snapshot{}
	}
	return Snapshot{Percent: sum / float64(count), OK: true}
}

func readCapacityPercent(dir string) (float64, bool) {
	if data, err := os.ReadFile(filepath.Join(dir, "capacity")); err == nil {
		if v, err := strconv.ParseFloat(strings.TrimSpace(string(data)), 64); err == nil && v >= 0 && v <= 100 {
			return v, true
		}
	}

	energyNow, okNow := readUint(dir, "energy_now")
	energyFull, okFull := readUint(dir, "energy_full")
	if okNow && okFull && energyFull > 0 {
		return float64(energyNow) / float64(energyFull) * 100, true
	}

	chargeNow, okChargeNow := readUint(dir, "charge_now")
	chargeFull, okChargeFull := readUint(dir, "charge_full")
	if okChargeNow && okChargeFull && chargeFull > 0 {
		return float64(chargeNow) / float64(chargeFull) * 100, true
	}

	return 0, false
}

func readUint(dir, name string) (uint64, bool) {
	data, err := os.ReadFile(filepath.Join(dir, name))
	if err != nil {
		return 0, false
	}
	v, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
	if err != nil {
		return 0, false
	}
	return v, true
}
