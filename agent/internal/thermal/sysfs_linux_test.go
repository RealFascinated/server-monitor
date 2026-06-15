//go:build linux

package thermal

import (
	"os"
	"path/filepath"
	"strconv"
	"testing"
)

func TestSysfsMilliCelsius(t *testing.T) {
	c, ok := sysfsMilliCelsius(46800)
	if !ok || c != 46.8 {
		t.Fatalf("millidegrees: got %v ok=%v", c, ok)
	}
	c, ok = sysfsMilliCelsius(46)
	if !ok || c != 46 {
		t.Fatalf("whole degrees: got %v ok=%v", c, ok)
	}
	if _, ok := sysfsMilliCelsius(0); ok {
		t.Fatal("expected invalid for zero")
	}
}

func TestReadHwmonTemperatures(t *testing.T) {
	root := t.TempDir()
	hwmon := filepath.Join(root, "sys", "class", "hwmon", "hwmon2")
	if err := os.MkdirAll(hwmon, 0o755); err != nil {
		t.Fatal(err)
	}
	writeThermalFile(t, filepath.Join(hwmon, "name"), "k10temp\n")
	writeThermalFile(t, filepath.Join(hwmon, "temp1_label"), "Tctl\n")
	writeThermalFile(t, filepath.Join(hwmon, "temp1_input"), "68500\n")

	t.Setenv("MONITOR_HOST_ROOT", root)
	resetSensorRegistry()
	readings := ReadTemperatures()
	if len(readings) != 1 {
		t.Fatalf("readings = %+v", readings)
	}
	if readings[0].Sensor != "k10temp/Tctl" {
		t.Fatalf("sensor = %q, want k10temp/Tctl", readings[0].Sensor)
	}
	if readings[0].Celsius < 68 || readings[0].Celsius > 69 {
		t.Fatalf("celsius = %v, want ~68.5", readings[0].Celsius)
	}
}

func TestReadThermalZoneTemperatures(t *testing.T) {
	root := t.TempDir()
	zone := filepath.Join(root, "sys", "class", "thermal", "thermal_zone0")
	if err := os.MkdirAll(zone, 0o755); err != nil {
		t.Fatal(err)
	}
	writeThermalFile(t, filepath.Join(zone, "type"), "x86_pkg_temp\n")
	writeThermalFile(t, filepath.Join(zone, "temp"), "52000\n")

	t.Setenv("MONITOR_HOST_ROOT", root)
	resetSensorRegistry()
	readings := ReadTemperatures()
	if len(readings) != 1 || readings[0].Sensor != "x86_pkg_temp" {
		t.Fatalf("readings = %+v", readings)
	}
	if readings[0].Celsius != 52 {
		t.Fatalf("celsius = %v", readings[0].Celsius)
	}
}

func TestDedupeTemperatureReadings(t *testing.T) {
	readings := dedupeTemperatureReadings([]TemperatureReading{
		{Sensor: "thermal_zone0", Celsius: 52},
		{Sensor: "coretemp/Package id 0", Celsius: 52.1},
	})
	if len(readings) != 1 || readings[0].Sensor != "coretemp/Package id 0" {
		t.Fatalf("deduped = %+v", readings)
	}
}

func TestDedupeKeepsDistinctHwmonSensors(t *testing.T) {
	readings := dedupeTemperatureReadings([]TemperatureReading{
		{Sensor: "k10temp/Tctl", Celsius: 68},
		{Sensor: "k10temp/Composite", Celsius: 67.5},
		{Sensor: "nvme0/Sensor 1", Celsius: 51},
		{Sensor: "nvme1/Sensor 1", Celsius: 50.5},
		{Sensor: "x86_pkg_temp", Celsius: 68.2},
	})
	if len(readings) != 4 {
		t.Fatalf("deduped = %+v, want 4 (drop x86_pkg_temp only)", readings)
	}
	for _, want := range []string{"k10temp/Tctl", "k10temp/Composite", "nvme0/Sensor 1", "nvme1/Sensor 1"} {
		found := false
		for _, r := range readings {
			if r.Sensor == want {
				found = true
				break
			}
		}
		if !found {
			t.Fatalf("missing %q in %+v", want, readings)
		}
	}
}

func TestIsReportedTemperatureLabel(t *testing.T) {
	if isReportedTemperatureLabel("Critical Temperature") {
		t.Fatal("expected false for critical threshold label")
	}
}

func TestDeviceIDFromPath(t *testing.T) {
	path := "/sys/devices/pci0000:00/0000:05:00.0/nvme/nvme0"
	if got := deviceIDFromPath(path); got != "nvme0" {
		t.Fatalf("nvme id = %q, want nvme0", got)
	}
	path = "/sys/devices/pci0000:00/0000:06:00.0/nvme/nvme1"
	if got := deviceIDFromPath(path); got != "nvme1" {
		t.Fatalf("nvme id = %q, want nvme1", got)
	}
	path = "/sys/devices/pci0000:00/0000:05:00.0/nvme/nvme0/device/hwmon/hwmon3"
	if got := deviceIDFromPath(path); got != "nvme0" {
		t.Fatalf("nvme hwmon path id = %q, want nvme0", got)
	}
}

func TestReadHwmonNVMeTemperatures(t *testing.T) {
	root := t.TempDir()
	if err := linkNVMeHwmon(t, root, "hwmon5", "nvme0", "Composite", 51000); err != nil {
		t.Fatal(err)
	}
	if err := linkNVMeHwmon(t, root, "hwmon6", "nvme1", "Composite", 47000); err != nil {
		t.Fatal(err)
	}
	// Same label and overlapping temps; without per-hwmon dedup the second drive is dropped.
	if err := linkNVMeHwmon(t, root, "hwmon7", "nvme1", "Sensor 1", 47000); err != nil {
		t.Fatal(err)
	}

	t.Setenv("MONITOR_HOST_ROOT", root)
	resetSensorRegistry()
	readings := ReadTemperatures()
	if len(readings) != 3 {
		t.Fatalf("readings = %+v", readings)
	}
	bySensor := map[string]float64{}
	for _, r := range readings {
		bySensor[r.Sensor] = r.Celsius
	}
	if bySensor["nvme0/Composite"] < 50 || bySensor["nvme0/Composite"] > 52 {
		t.Fatalf("nvme0 temp = %v", bySensor["nvme0/Composite"])
	}
	if bySensor["nvme1/Composite"] < 46 || bySensor["nvme1/Composite"] > 48 {
		t.Fatalf("nvme1 temp = %v", bySensor["nvme1/Composite"])
	}
	if bySensor["nvme1/Sensor 1"] < 46 || bySensor["nvme1/Sensor 1"] > 48 {
		t.Fatalf("nvme1 Sensor 1 temp = %v", bySensor["nvme1/Sensor 1"])
	}
}

func linkNVMeHwmon(t *testing.T, root, hwmonName, nvmeName, label string, milli int64) error {
	t.Helper()
	device := filepath.Join(root, "sys", "devices", "pci0000", "0000:05:00.0", "nvme", nvmeName)
	classNVMe := filepath.Join(root, "sys", "class", "nvme", nvmeName)
	hwmonClass := filepath.Join(root, "sys", "class", "hwmon", hwmonName)
	hwmonOnNVMe := filepath.Join(classNVMe, "device", "hwmon", hwmonName)

	for _, dir := range []string{device, classNVMe, hwmonClass, hwmonOnNVMe} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return err
		}
	}
	for _, hwmon := range []string{hwmonClass, hwmonOnNVMe} {
		relDevice, err := filepath.Rel(hwmon, device)
		if err != nil {
			return err
		}
		deviceLink := filepath.Join(hwmon, "device")
		_ = os.Remove(deviceLink)
		if err := os.Symlink(relDevice, deviceLink); err != nil {
			return err
		}
		writeThermalFile(t, filepath.Join(hwmon, "name"), "nvme\n")
		writeThermalFile(t, filepath.Join(hwmon, "temp2_label"), label+"\n")
		writeThermalFile(t, filepath.Join(hwmon, "temp2_input"), strconv.FormatInt(milli, 10)+"\n")
	}
	return nil
}

func writeThermalFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}
