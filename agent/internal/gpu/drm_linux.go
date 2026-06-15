//go:build linux

package gpu

import (
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

const (
	vendorAMD    = "0x1002"
	vendorIntel  = "0x8086"
	vendorNVIDIA = "0x10de"
)

var (
	cardDirPattern  = regexp.MustCompile(`^card\d+$`)
	pciSlotPattern  = regexp.MustCompile(`^[0-9a-f]{4}:[0-9a-f]{2}:[0-9a-f]{2}\.[0-7]$`)
)

func collectDRM() []ingest.GPUMetric {
	drmRoot := linux.HostPath("/sys/class/drm")
	entries, err := os.ReadDir(drmRoot)
	if err != nil {
		return nil
	}

	var metrics []ingest.GPUMetric
	for _, entry := range entries {
		if !cardDirPattern.MatchString(entry.Name()) {
			continue
		}
		deviceDir := filepath.Join(drmRoot, entry.Name(), "device")
		if _, err := os.Stat(deviceDir); err != nil {
			continue
		}

		vendor := strings.ToLower(readTrimmed(filepath.Join(deviceDir, "vendor")))
		if vendor == vendorNVIDIA {
			continue
		}

		vendorName, driver := drmVendorName(vendor, deviceDir)
		if vendorName == "" {
			continue
		}
		if !isGPUDriver(driver) {
			continue
		}
		if !hasGPUMetrics(deviceDir, vendorName) {
			continue
		}

		rawID := drmDeviceID(deviceDir)
		if rawID == "" {
			continue
		}
		metric := ingest.GPUMetric{
			DeviceID: ingest.HashDeviceID(rawID),
			Vendor:   vendorName,
			Name:     drmDeviceName(deviceDir, entry.Name(), rawID, vendorName),
		}

		if usage, ok := readGPUUsage(deviceDir, vendorName); ok {
			metric.UsagePercent = usage
		}
		if used, ok := readInt64(filepath.Join(deviceDir, "mem_info_vram_used")); ok {
			metric.MemoryUsedBytes = used
		}
		if total, ok := readInt64(filepath.Join(deviceDir, "mem_info_vram_total")); ok {
			metric.MemoryTotalBytes = total
		}
		if temp, ok := readGPUHwmon(deviceDir, "temp"); ok {
			metric.TemperatureCelsius = temp
		}
		if power, ok := readGPUHwmon(deviceDir, "power"); ok {
			metric.PowerWatts = power
		}

		metrics = append(metrics, metric)
	}
	if len(metrics) == 0 {
		return nil
	}
	return metrics
}

func drmVendorName(vendor, deviceDir string) (string, string) {
	driver := drmDriverName(deviceDir)
	switch vendor {
	case vendorAMD:
		return "amd", driver
	case vendorIntel:
		return "intel", driver
	default:
		return "", driver
	}
}

func drmDriverName(deviceDir string) string {
	link := filepath.Join(deviceDir, "driver")
	target, err := os.Readlink(link)
	if err != nil {
		return readUeventDriver(filepath.Join(deviceDir, "uevent"))
	}
	return filepath.Base(target)
}

func readUeventDriver(path string) string {
	data, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	for _, line := range strings.Split(string(data), "\n") {
		if strings.HasPrefix(line, "DRIVER=") {
			return strings.TrimPrefix(line, "DRIVER=")
		}
	}
	return ""
}

func isGPUDriver(driver string) bool {
	switch driver {
	case "amdgpu", "i915", "xe":
		return true
	default:
		return false
	}
}

func hasGPUMetrics(deviceDir, vendor string) bool {
	if _, ok := readInt64(filepath.Join(deviceDir, "gpu_busy_percent")); ok {
		return true
	}
	if _, ok := readInt64(filepath.Join(deviceDir, "mem_info_vram_total")); ok {
		return true
	}
	if vendor == "intel" {
		for _, path := range intelBusyPaths(deviceDir) {
			if _, ok := readInt64(path); ok {
				return true
			}
		}
	}
	if _, ok := readGPUHwmon(deviceDir, "temp"); ok {
		return true
	}
	return false
}

func intelBusyPaths(deviceDir string) []string {
	return []string{
		filepath.Join(deviceDir, "gt", "gt0", "gt_busy_percent"),
		filepath.Join(deviceDir, "gt", "gt1", "gt_busy_percent"),
	}
}

func readGPUUsage(deviceDir, vendor string) (float64, bool) {
	if value, ok := readInt64(filepath.Join(deviceDir, "gpu_busy_percent")); ok {
		return float64(value), true
	}
	if vendor == "intel" {
		for _, path := range intelBusyPaths(deviceDir) {
			if value, ok := readInt64(path); ok {
				return float64(value), true
			}
		}
	}
	return 0, false
}

func drmDeviceID(deviceDir string) string {
	if slot := readUeventValue(filepath.Join(deviceDir, "uevent"), "PCI_SLOT_NAME"); pciSlotPattern.MatchString(slot) {
		return strings.ToLower(slot)
	}
	resolved, err := filepath.EvalSymlinks(deviceDir)
	if err != nil {
		return ""
	}
	base := filepath.Base(resolved)
	if pciSlotPattern.MatchString(base) {
		return strings.ToLower(base)
	}
	return ""
}

func readUeventValue(path, key string) string {
	data, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	prefix := key + "="
	for _, line := range strings.Split(string(data), "\n") {
		if strings.HasPrefix(line, prefix) {
			return strings.TrimSpace(strings.TrimPrefix(line, prefix))
		}
	}
	return ""
}

func drmDeviceName(deviceDir, cardName, pciSlot, vendor string) string {
	if name := readTrimmed(filepath.Join(deviceDir, "product_name")); name != "" {
		return name
	}
	if name := readTrimmed(filepath.Join(deviceDir, "product_number")); name != "" {
		return name
	}
	if vendor == "amd" {
		if name := amdgpuDeviceName(deviceDir); name != "" {
			return name
		}
	}
	if name := pciDeviceName(pciSlot); name != "" {
		return name
	}
	return cardName
}

func readGPUHwmon(deviceDir, kind string) (float64, bool) {
	hwmonRoot := filepath.Join(deviceDir, "hwmon")
	entries, err := os.ReadDir(hwmonRoot)
	if err != nil {
		return 0, false
	}

	for _, entry := range entries {
		if !strings.HasPrefix(entry.Name(), "hwmon") {
			continue
		}
		dir := filepath.Join(hwmonRoot, entry.Name())
		switch kind {
		case "temp":
			if value, ok := readBestTemperature(dir); ok {
				return value, true
			}
		case "power":
			if value, ok := readBestPower(dir); ok {
				return value, true
			}
		}
	}
	return 0, false
}

func readBestTemperature(hwmonDir string) (float64, bool) {
	priority := []string{"temp3_input", "temp2_input", "temp1_input"}
	for _, name := range priority {
		value, ok := readMillidegreeCelsius(filepath.Join(hwmonDir, name))
		if ok {
			return value, true
		}
	}
	return 0, false
}

func readMillidegreeCelsius(path string) (float64, bool) {
	milli, ok := readInt64(path)
	if !ok || milli <= 0 {
		return 0, false
	}
	return float64(milli) / 1000, true
}

func readBestPower(hwmonDir string) (float64, bool) {
	var best float64
	var found bool
	for _, name := range []string{
		"power1_average", "power2_average",
		"power1_input", "power2_input",
	} {
		micro, ok := readInt64(filepath.Join(hwmonDir, name))
		if !ok || micro <= 0 {
			continue
		}
		watts := float64(micro) / 1_000_000
		if !found || watts > best {
			best = watts
			found = true
		}
	}
	return best, found
}
