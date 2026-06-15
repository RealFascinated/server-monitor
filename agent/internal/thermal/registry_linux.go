//go:build linux

package thermal

import (
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/linux"
)

const sensorRescanInterval = 5 * time.Minute

type cachedSensor struct {
	tempPath   string
	sensorName string
}

var (
	registryMu   sync.Mutex
	registryAt   time.Time
	hwmonSensors []cachedSensor
	zoneSensors  []cachedSensor
)

func ReadTemperatures() []TemperatureReading {
	refreshSensorRegistry()

	registryMu.Lock()
	hwmon := append([]cachedSensor(nil), hwmonSensors...)
	zones := append([]cachedSensor(nil), zoneSensors...)
	registryMu.Unlock()

	var readings []TemperatureReading
	for _, sensor := range hwmon {
		if celsius, ok := readTemperatureFile(sensor.tempPath); ok {
			readings = append(readings, TemperatureReading{Sensor: sensor.sensorName, Celsius: celsius})
		}
	}
	for _, sensor := range zones {
		if celsius, ok := readTemperatureFile(sensor.tempPath); ok {
			readings = append(readings, TemperatureReading{Sensor: sensor.sensorName, Celsius: celsius})
		}
	}

	readings = dedupeTemperatureReadings(readings)
	if len(readings) == 0 {
		return nil
	}
	sortTemperatures(readings)
	return readings
}

func refreshSensorRegistry() {
	registryMu.Lock()
	defer registryMu.Unlock()

	if !registryAt.IsZero() && time.Since(registryAt) < sensorRescanInterval {
		return
	}
	registryAt = time.Now()
	hwmonSensors = discoverHwmonSensors()
	zoneSensors = discoverZoneSensors()
}

func discoverZoneSensors() []cachedSensor {
	entries, err := os.ReadDir(sysPath("class", "thermal"))
	if err != nil {
		return nil
	}

	var sensors []cachedSensor
	for _, entry := range entries {
		name := entry.Name()
		if !strings.HasPrefix(name, "thermal_zone") {
			continue
		}
		zoneDir := sysPath("class", "thermal", name)
		sensor := name
		if zoneType := readTrimmedFile(filepath.Join(zoneDir, "type")); zoneType != "" {
			sensor = zoneType
		}
		sensors = append(sensors, cachedSensor{
			tempPath:   filepath.Join(zoneDir, "temp"),
			sensorName: sensor,
		})
	}
	return sensors
}

func discoverHwmonSensors() []cachedSensor {
	var inputs []string
	for _, pattern := range hwmonTemperatureGlobPatterns() {
		matches, err := filepath.Glob(linux.HostPath(pattern))
		if err != nil {
			continue
		}
		inputs = append(inputs, matches...)
	}
	if len(inputs) == 0 {
		return nil
	}

	var sensors []cachedSensor
	seenPath := make(map[string]struct{}, len(inputs))
	seenPhysical := make(map[string]struct{})
	for _, inputPath := range inputs {
		resolved := inputPath
		if abs, err := filepath.EvalSymlinks(inputPath); err == nil {
			resolved = abs
		}
		if _, ok := seenPath[resolved]; ok {
			continue
		}
		seenPath[resolved] = struct{}{}

		file := filepath.Base(inputPath)
		if !strings.HasSuffix(file, "_input") {
			continue
		}
		basename := strings.TrimSuffix(file, "_input")
		dir := filepath.Dir(inputPath)

		label := readTrimmedFile(filepath.Join(dir, basename+"_label"))
		if !isReportedTemperatureLabel(label) {
			continue
		}

		hwmonDir := hwmonRootDir(dir)
		hwName := readTrimmedFile(filepath.Join(hwmonDir, "name"))

		physicalKey := hwmonPhysicalDedupKey(hwName, dir, basename, label)
		if _, ok := seenPhysical[physicalKey]; ok {
			continue
		}
		seenPhysical[physicalKey] = struct{}{}

		sensors = append(sensors, cachedSensor{
			tempPath:   inputPath,
			sensorName: hwmonSensorKey(hwName, dir, basename, label),
		})
	}
	return sensors
}

func sortTemperatures(readings []TemperatureReading) {
	sort.Slice(readings, func(i, j int) bool {
		return readings[i].Sensor < readings[j].Sensor
	})
}

func resetSensorRegistry() {
	registryMu.Lock()
	defer registryMu.Unlock()
	registryAt = time.Time{}
	hwmonSensors = nil
	zoneSensors = nil
}
