package smart

import (
	"errors"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
)

const scanJSON = `{"json_format_version":[1,0],"devices":[
 {"name":"/dev/sda","info_name":"/dev/sda [SAT]","type":"sat","protocol":"ATA"},
 {"name":"/dev/nvme0","info_name":"/dev/nvme0","type":"nvme","protocol":"NVMe"}]}`

const sataJSON = `{
 "device":{"name":"/dev/sda","type":"sat","protocol":"ATA"},
 "model_name":"WDC WD40EFRX-68N32N0 ",
 "serial_number":"WD-SECRET123",
 "smart_status":{"passed":true},
 "temperature":{"current":34},
 "power_on_time":{"hours":41234},
 "ata_smart_attributes":{"table":[
  {"id":1,"name":"Raw_Read_Error_Rate","raw":{"value":0}},
  {"id":5,"name":"Reallocated_Sector_Ct","raw":{"value":8}},
  {"id":197,"name":"Current_Pending_Sector","raw":{"value":2}},
  {"id":9,"name":"Power_On_Hours","raw":{"value":41234}}]},
 "smartctl":{"exit_status":0}}`

const nvmeJSON = `{
 "device":{"name":"/dev/nvme0","type":"nvme","protocol":"NVMe"},
 "model_name":"Samsung SSD 980 PRO 1TB",
 "serial_number":"S5GXNX0T123456",
 "smart_status":{"passed":false},
 "temperature":{"current":47},
 "power_on_time":{"hours":1200},
 "nvme_smart_health_information_log":{
  "critical_warning":0,"temperature":47,"available_spare":96,"available_spare_threshold":10,
  "percentage_used":4,"data_units_read":1000,"data_units_written":2000,
  "power_cycles":55,"power_on_hours":1200,"unsafe_shutdowns":7,"media_errors":3}}`

const standbyJSON = `{"smartctl":{"exit_status":2,"messages":[{"string":"Device is in STANDBY mode, exit(2)","severity":"information"}]}}`

func TestParseDeviceSata(t *testing.T) {
	metric, ok := parseDevice([]byte(sataJSON), "sda")
	if !ok {
		t.Fatal("expected a reading")
	}
	if metric.Device != "sda" || metric.Model != "WDC WD40EFRX-68N32N0" {
		t.Fatalf("identity = %q / %q", metric.Device, metric.Model)
	}
	if metric.HealthPassed == nil || !*metric.HealthPassed {
		t.Fatalf("health = %v", metric.HealthPassed)
	}
	if *metric.TemperatureCelsius != 34 || *metric.PowerOnHours != 41234 {
		t.Fatalf("temperature/hours = %v/%v", *metric.TemperatureCelsius, *metric.PowerOnHours)
	}
	if *metric.ReallocatedSectors != 8 || *metric.PendingSectors != 2 {
		t.Fatalf("sectors = %v/%v", *metric.ReallocatedSectors, *metric.PendingSectors)
	}
	if metric.PercentageUsed != nil || metric.MediaErrors != nil {
		t.Fatal("NVMe fields must be empty for a SATA disk")
	}
}

func TestParseDeviceNeverReportsTheSerialNumber(t *testing.T) {
	metric, _ := parseDevice([]byte(sataJSON), "sda")
	if strings.Contains(strings.ToLower(metric.Model+metric.Device), "secret") {
		t.Fatalf("serial number present in %+v", metric)
	}
}

func TestParseDeviceNvme(t *testing.T) {
	metric, ok := parseDevice([]byte(nvmeJSON), "nvme0")
	if !ok {
		t.Fatal("expected a reading")
	}
	if metric.HealthPassed == nil || *metric.HealthPassed {
		t.Fatalf("health = %v, want a failed status to be reported as false", metric.HealthPassed)
	}
	if *metric.PercentageUsed != 4 || *metric.AvailableSpare != 96 || *metric.MediaErrors != 3 || *metric.UnsafeShutdowns != 7 {
		t.Fatalf("nvme fields = %+v", metric)
	}
	if *metric.DataWrittenBytes != 2000*512_000 {
		t.Fatalf("data written = %d bytes, want 2000 data units of 512000", *metric.DataWrittenBytes)
	}
	if *metric.DataReadBytes != 1000*512_000 {
		t.Fatalf("data read = %d bytes, want 1000 data units of 512000", *metric.DataReadBytes)
	}
	if metric.ReallocatedSectors != nil {
		t.Fatal("SATA fields must be empty for an NVMe disk")
	}
}

func TestParseDeviceWithoutHealthData(t *testing.T) {
	for name, out := range map[string]string{"standby": standbyJSON, "not json": "Permission denied", "empty": ""} {
		if _, ok := parseDevice([]byte(out), "sda"); ok {
			t.Fatalf("%s: expected no reading", name)
		}
	}
}

func TestParseScan(t *testing.T) {
	devices := parseScan([]byte(scanJSON))
	if len(devices) != 2 || devices[0].name != "/dev/sda" || devices[0].kind != "sat" || devices[1].kind != "nvme" {
		t.Fatalf("devices = %+v", devices)
	}
	if parseScan([]byte("nonsense")) != nil {
		t.Fatal("expected nothing from invalid output")
	}
}

type fakeSmartctl struct {
	sda     string
	calls   [][]string
	scanErr error
}

func (f *fakeSmartctl) run(args ...string) ([]byte, error) {
	f.calls = append(f.calls, args)
	if args[0] == "--scan" {
		if f.scanErr != nil {
			return nil, f.scanErr
		}
		return []byte(scanJSON), nil
	}
	switch args[len(args)-1] {
	case "/dev/sda":
		// smartctl exits non-zero when it found a problem but still prints valid JSON.
		return []byte(f.sda), errors.New("exit status 4")
	case "/dev/nvme0":
		return []byte(nvmeJSON), nil
	}
	return nil, errors.New("unexpected device")
}

func newTestCollector(f *fakeSmartctl, clock *time.Time) *collector {
	return &collector{run: f.run, now: func() time.Time { return *clock }, last: map[string]reading{}}
}

func TestCollectQueriesEveryDeviceWithoutWakingSleepingDisks(t *testing.T) {
	fake := &fakeSmartctl{sda: sataJSON}
	now := time.Unix(1_700_000_000, 0)

	metrics := newTestCollector(fake, &now).collect()

	if len(metrics) != 2 || metrics[0].Device != "sda" || metrics[1].Device != "nvme0" {
		t.Fatalf("metrics = %+v", metrics)
	}
	query := strings.Join(fake.calls[1], " ")
	if query != "-a --json -n standby -d sat /dev/sda" {
		t.Fatalf("query = %q, want standby-safe invocation with the scanned device type", query)
	}
}

func TestCollectKeepsPreviousReadingWhileDiskSleeps(t *testing.T) {
	fake := &fakeSmartctl{sda: sataJSON}
	now := time.Unix(1_700_000_000, 0)
	c := newTestCollector(fake, &now)
	c.collect()

	fake.sda = standbyJSON
	now = now.Add(10 * time.Minute)
	metrics := c.collect()
	if len(metrics) != 2 || metrics[0].Device != "sda" {
		t.Fatalf("metrics = %+v, want the sleeping disk's last reading kept", metrics)
	}

	now = now.Add(3 * time.Hour)
	metrics = c.collect()
	if len(metrics) != 1 || metrics[0].Device != "nvme0" {
		t.Fatalf("metrics = %+v, want a stale reading dropped", metrics)
	}
}

func TestCollectForgetsDisksThatDisappear(t *testing.T) {
	fake := &fakeSmartctl{sda: sataJSON}
	now := time.Unix(1_700_000_000, 0)
	c := newTestCollector(fake, &now)
	c.collect()
	if len(c.last) != 2 {
		t.Fatalf("remembered %d disks", len(c.last))
	}

	c.last["sdz"] = reading{at: now}
	c.collect()
	if _, still := c.last["sdz"]; still {
		t.Fatal("a disk missing from the scan must be forgotten")
	}
}

func TestCollectWithoutSmartctlReportsNothing(t *testing.T) {
	fake := &fakeSmartctl{scanErr: errors.New(`exec: "smartctl": executable file not found in $PATH`)}
	now := time.Unix(1_700_000_000, 0)

	if metrics := newTestCollector(fake, &now).collect(); metrics != nil {
		t.Fatalf("metrics = %+v, want none", metrics)
	}
}

func resetState() {
	state.mu.Lock()
	state.at = time.Time{}
	state.refreshing = false
	state.metrics = nil
	state.mu.Unlock()
}

func TestReadRefreshesInTheBackgroundAndOnlyOnce(t *testing.T) {
	resetState()
	defer resetState()
	original := collect
	defer func() { collect = original }()

	var runs atomic.Int32
	release := make(chan struct{})
	done := make(chan struct{})
	collect = func() []ingest.SmartMetric {
		runs.Add(1)
		<-release
		defer close(done)
		return []ingest.SmartMetric{{Device: "sda"}}
	}

	if got := Read(); got != nil {
		t.Fatalf("first read = %+v, want nothing until the background refresh finishes", got)
	}
	Read() // a second call while refreshing must not start another query
	close(release)
	<-done

	deadline := time.After(5 * time.Second)
	for {
		if got := Read(); len(got) == 1 {
			break
		}
		select {
		case <-deadline:
			t.Fatal("refresh result never became visible")
		case <-time.After(5 * time.Millisecond):
		}
	}
	if runs.Load() != 1 {
		t.Fatalf("collect ran %d times, want 1", runs.Load())
	}
}
