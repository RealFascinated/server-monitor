//go:build linux

package thermal

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

func hwmonTemperatureGlobPatterns() []string {
	return []string{
		"/sys/class/hwmon/hwmon*/temp*_input",
		"/sys/class/hwmon/hwmon*/device/temp*_input",
		"/sys/class/nvme/nvme*/device/hwmon/hwmon*/temp*_input",
		"/sys/class/nvme/nvme*/device/hwmon/hwmon*/device/temp*_input",
	}
}

func hwmonPhysicalDedupKey(hwName, dir, basename, label string) string {
	key := hwmonPhysicalKey(hwName, dir, basename, label)
	if hwName == "nvme" {
		return filepath.Base(hwmonRootDir(dir)) + "/" + key
	}
	return key
}

func hwmonPhysicalKey(hwName, dir, basename, label string) string {
	if label == "" {
		label = basename
	}
	deviceID := hwmonDeviceID(dir)
	if usesDeviceScopedHwmonName(hwName) || deviceID != filepath.Base(hwmonRootDir(dir)) {
		return deviceID + "/" + label
	}
	return hwName + "/" + label
}

func hwmonSensorKey(hwName, dir, basename, label string) string {
	if label == "" {
		label = basename
	}
	prefix := hwName
	if prefix == "" {
		prefix = filepath.Base(hwmonRootDir(dir))
	}
	if prefix == "nvme" {
		if id := nvmeControllerID(dir); id != "" {
			prefix = id
		} else if deviceID := hwmonDeviceID(dir); deviceID != "" {
			prefix = deviceID
		}
	} else if usesDeviceScopedHwmonName(prefix) {
		if deviceID := hwmonDeviceID(dir); deviceID != "" {
			prefix = deviceID
		}
	}
	return prefix + "/" + label
}

func dedupeTemperatureReadings(readings []TemperatureReading) []TemperatureReading {
	if len(readings) < 2 {
		return readings
	}
	out := make([]TemperatureReading, 0, len(readings))
	for _, reading := range readings {
		if keepTemperatureReading(reading, readings) {
			out = append(out, reading)
		}
	}
	return out
}

func keepTemperatureReading(candidate TemperatureReading, all []TemperatureReading) bool {
	for _, other := range all {
		if other.Sensor == candidate.Sensor {
			continue
		}
		if !temperaturesClose(candidate.Celsius, other.Celsius) {
			continue
		}
		// Only drop redundant thermal_zone / ACPI-style readings when hwmon reports the same temp.
		if isRedundantThermalReading(candidate.Sensor, other.Sensor) {
			return false
		}
	}
	return true
}

// isRedundantThermalReading reports whether zoneSensor duplicates hwmonSensor.
func isRedundantThermalReading(zoneSensor, hwmonSensor string) bool {
	if strings.Contains(zoneSensor, "/") {
		return false
	}
	return strings.Contains(hwmonSensor, "/")
}

func temperaturesClose(a, b float64) bool {
	const threshold = 2.0
	diff := a - b
	if diff < 0 {
		diff = -diff
	}
	return diff <= threshold
}

func isReportedTemperatureLabel(label string) bool {
	if label == "" {
		return true
	}
	lower := strings.ToLower(label)
	if strings.Contains(lower, "distance") {
		return false
	}
	if strings.Contains(lower, "warning temperature") {
		return false
	}
	if strings.Contains(lower, "critical temperature") {
		return false
	}
	return true
}

func readTemperatureFile(path string) (float64, bool) {
	data, err := os.ReadFile(path)
	if err != nil {
		return 0, false
	}
	raw, err := strconv.ParseInt(strings.TrimSpace(string(data)), 10, 64)
	if err != nil {
		return 0, false
	}
	celsius, ok := sysfsMilliCelsius(raw)
	if !ok {
		return 0, false
	}
	return celsius, isReportedTemperature(celsius)
}

// sysfsMilliCelsius converts a sysfs temperature reading to degrees Celsius.
// Most drivers use millidegree Celsius; some report whole degrees when raw < 1000.
func sysfsMilliCelsius(raw int64) (float64, bool) {
	if raw <= 0 {
		return 0, false
	}
	switch {
	case raw >= 1000:
		return float64(raw) / 1000, true
	case raw <= 150:
		return float64(raw), true
	default:
		return float64(raw) / 1000, true
	}
}

func isReportedTemperature(celsius float64) bool {
	return celsius > 0 && celsius <= 150
}

func readTrimmedFile(path string) string {
	data, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(data))
}

func sysPath(elem ...string) string {
	return linux.HostPath(filepath.Join(append([]string{"/sys"}, elem...)...))
}
