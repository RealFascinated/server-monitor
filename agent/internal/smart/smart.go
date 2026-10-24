// Package smart reports disk health from smartctl (smartmontools). It is optional: without
// smartctl, or without permission to open the disks, it reports nothing.
package smart

import (
	"encoding/json"
	"log/slog"
	"strings"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/executil"
	"fascinated.cc/monitor/agent/internal/ingest"
)

const (
	// refreshEvery keeps disks from being queried on every slow refresh; SMART data changes slowly.
	refreshEvery = 10 * time.Minute
	// commandTimeout is generous because a spun-down disk can take several seconds to answer.
	commandTimeout = 30 * time.Second
	maxDevices     = 64
	// staleAfter bounds how long a previous reading stands in for a disk that is asleep.
	staleAfter = 2 * time.Hour
	// nvmeDataUnitBytes is the size of one NVMe "data unit" (1000 * 512 bytes).
	nvmeDataUnitBytes = 512_000
)

type runner func(name string, args ...string) ([]byte, error)

func runSmartctl(args ...string) ([]byte, error) {
	return executil.CommandOutputTimeout(commandTimeout, "smartctl", args...)
}

type reading struct {
	metric ingest.SmartMetric
	at     time.Time
}

// collector queries smartctl and remembers the last reading of each disk.
type collector struct {
	run  func(args ...string) ([]byte, error)
	now  func() time.Time
	last map[string]reading
}

var state struct {
	mu         sync.Mutex
	at         time.Time
	refreshing bool
	metrics    []ingest.SmartMetric
	col        *collector
	warned     sync.Once
}

// collect is swapped by tests.
var collect = func() []ingest.SmartMetric {
	if state.col == nil {
		state.col = &collector{run: runSmartctl, now: time.Now, last: map[string]reading{}}
	}
	return state.col.collect()
}

// Read returns the most recent SMART readings. Querying disks can take a while, so it happens in
// the background on a ten minute cadence and callers always get the last result immediately.
func Read() []ingest.SmartMetric {
	state.mu.Lock()
	defer state.mu.Unlock()

	if !state.refreshing && (state.at.IsZero() || time.Since(state.at) >= refreshEvery) {
		state.refreshing = true
		go refresh()
	}
	return state.metrics
}

func refresh() {
	metrics := collect()

	state.mu.Lock()
	defer state.mu.Unlock()
	state.metrics = metrics
	state.at = time.Now()
	state.refreshing = false
}

func (c *collector) collect() []ingest.SmartMetric {
	out, err := c.run("--scan", "--json")
	if len(out) == 0 {
		if err != nil {
			state.warned.Do(func() { slog.Debug("smartctl unavailable, SMART metrics disabled", "err", err) })
		}
		return nil
	}

	devices := parseScan(out)
	if len(devices) > maxDevices {
		devices = devices[:maxDevices]
	}

	now := c.now()
	metrics := make([]ingest.SmartMetric, 0, len(devices))
	seen := make(map[string]struct{}, len(devices))
	for _, device := range devices {
		name := strings.TrimPrefix(device.name, "/dev/")
		seen[name] = struct{}{}

		metric, ok := c.query(device, name, false)
		if !ok && device.kind != "" {
			// The type from --scan is only a hint, and for SATA disks behind an HBA it is usually
			// "scsi", which answers with SCSI pages and no ATA attributes at all.
			metric, ok = c.query(device, name, true)
		}

		if ok {
			c.last[name] = reading{metric: metric, at: now}
			metrics = append(metrics, metric)
		} else if previous, found := c.last[name]; found && now.Sub(previous.at) < staleAfter {
			// A disk in standby is skipped by -n standby; keep showing what we knew.
			metrics = append(metrics, previous.metric)
		}
	}
	for name := range c.last {
		if _, ok := seen[name]; !ok {
			delete(c.last, name)
		}
	}
	return metrics
}

// query reads one device with the standby-safe arguments. useKind pins the device type that
// --scan reported, which some SATA bridges need and others must not be given.
func (c *collector) query(device scanDevice, name string, useKind bool) (ingest.SmartMetric, bool) {
	args := []string{"-a", "--json", "-n", "standby"}
	if useKind && device.kind != "" {
		args = append(args, "-d", device.kind)
	}
	// smartctl exits non-zero for many harmless reasons (a failing attribute, a log it could
	// not read), so a usable JSON document matters more than the exit status.
	out, _ := c.run(append(args, device.name)...)
	return parseDevice(out, name)
}

type scanDevice struct {
	name string
	kind string
}

func parseScan(out []byte) []scanDevice {
	var scan struct {
		Devices []struct {
			Name string `json:"name"`
			Type string `json:"type"`
		} `json:"devices"`
	}
	if err := json.Unmarshal(out, &scan); err != nil {
		return nil
	}
	devices := make([]scanDevice, 0, len(scan.Devices))
	for _, device := range scan.Devices {
		if device.Name != "" {
			devices = append(devices, scanDevice{name: device.Name, kind: device.Type})
		}
	}
	return devices
}

type deviceOutput struct {
	ModelName   string `json:"model_name"`
	SmartStatus *struct {
		Passed bool `json:"passed"`
	} `json:"smart_status"`
	Temperature *struct {
		Current float64 `json:"current"`
	} `json:"temperature"`
	PowerOnTime *struct {
		Hours int64 `json:"hours"`
	} `json:"power_on_time"`
	PowerCycleCount *int64 `json:"power_cycle_count"`
	EnduranceUsed   *struct {
		CurrentPercent *int64 `json:"current_percent"`
	} `json:"endurance_used"`
	AtaSmartAttributes *struct {
		Table []struct {
			ID  int `json:"id"`
			Raw struct {
				Value int64 `json:"value"`
			} `json:"raw"`
		} `json:"table"`
	} `json:"ata_smart_attributes"`
	AtaErrorLog *struct {
		Summary *struct {
			Count *int64 `json:"count"`
		} `json:"summary"`
	} `json:"ata_smart_error_log"`
	Nvme *struct {
		PercentageUsed   *int64 `json:"percentage_used"`
		AvailableSpare   *int64 `json:"available_spare"`
		MediaErrors      *int64 `json:"media_errors"`
		UnsafeShutdowns  *int64 `json:"unsafe_shutdowns"`
		DataUnitsWritten *int64 `json:"data_units_written"`
		DataUnitsRead    *int64 `json:"data_units_read"`
	} `json:"nvme_smart_health_information_log"`
}

// Attributes are tracked only where smartctl reports a plain counter. Seagate packs several
// attributes (raw read and seek error rates, command timeout, head flying hours, its temperature
// attribute) into one 48-bit raw value, so their numbers would be meaningless here; the top-level
// temperature and power-on fields carry those readings already.
const (
	attrStartStopCount     = 4
	attrReallocatedSectors = 5
	attrHeliumLevel        = 22
	attrReportedUncorrect  = 187
	attrPowerOffRetracts   = 192
	attrLoadCycleCount     = 193
	attrReallocatedEvents  = 196
	attrPendingSectors     = 197
	attrOfflineUncorrect   = 198
	attrUdmaCrcErrors      = 199
	attrTotalLbasWritten   = 241
	attrTotalLbasRead      = 242

	// lbaBytes is the sector size the ATA total LBA attributes count in.
	lbaBytes = 512
)

// parseDevice converts one `smartctl -a --json` document. It reports false when the output
// carries no health information (disk asleep, unreadable, not JSON).
func parseDevice(out []byte, name string) (ingest.SmartMetric, bool) {
	var doc deviceOutput
	if err := json.Unmarshal(out, &doc); err != nil {
		return ingest.SmartMetric{}, false
	}
	if doc.SmartStatus == nil && doc.Nvme == nil && doc.AtaSmartAttributes == nil {
		return ingest.SmartMetric{}, false
	}

	// The serial number is deliberately never read: it identifies the hardware, and nothing
	// downstream needs it. The same applies to the world wide name, which encodes it.
	metric := ingest.SmartMetric{Device: name, Model: strings.TrimSpace(doc.ModelName)}
	if doc.SmartStatus != nil {
		passed := doc.SmartStatus.Passed
		metric.HealthPassed = &passed
	}
	if doc.Temperature != nil {
		temperature := doc.Temperature.Current
		metric.TemperatureCelsius = &temperature
	}
	if doc.PowerOnTime != nil {
		hours := doc.PowerOnTime.Hours
		metric.PowerOnHours = &hours
	}
	metric.PowerCycleCount = doc.PowerCycleCount
	if doc.EnduranceUsed != nil {
		metric.EnduranceUsedPercent = doc.EnduranceUsed.CurrentPercent
	}
	if doc.AtaErrorLog != nil && doc.AtaErrorLog.Summary != nil {
		metric.ErrorLogEntries = doc.AtaErrorLog.Summary.Count
	}
	if doc.AtaSmartAttributes != nil {
		for _, attribute := range doc.AtaSmartAttributes.Table {
			value := attribute.Raw.Value
			switch attribute.ID {
			case attrStartStopCount:
				metric.StartStopCounts = &value
			case attrReallocatedSectors:
				metric.ReallocatedSectors = &value
			case attrHeliumLevel:
				metric.HeliumPercent = &value
			case attrReportedUncorrect:
				metric.ReportedUncorrect = &value
			case attrPowerOffRetracts:
				metric.PowerOffRetracts = &value
			case attrLoadCycleCount:
				metric.LoadCycleCount = &value
			case attrReallocatedEvents:
				metric.ReallocatedEvents = &value
			case attrPendingSectors:
				metric.PendingSectors = &value
			case attrOfflineUncorrect:
				metric.OfflineUncorrectable = &value
			case attrUdmaCrcErrors:
				metric.UdmaCrcErrors = &value
			case attrTotalLbasWritten:
				written := value * lbaBytes
				metric.DataWrittenBytes = &written
			case attrTotalLbasRead:
				read := value * lbaBytes
				metric.DataReadBytes = &read
			}
		}
	}
	if doc.Nvme != nil {
		metric.PercentageUsed = doc.Nvme.PercentageUsed
		metric.AvailableSpare = doc.Nvme.AvailableSpare
		metric.MediaErrors = doc.Nvme.MediaErrors
		metric.UnsafeShutdowns = doc.Nvme.UnsafeShutdowns
		if doc.Nvme.DataUnitsWritten != nil {
			written := *doc.Nvme.DataUnitsWritten * nvmeDataUnitBytes
			metric.DataWrittenBytes = &written
		}
		if doc.Nvme.DataUnitsRead != nil {
			read := *doc.Nvme.DataUnitsRead * nvmeDataUnitBytes
			metric.DataReadBytes = &read
		}
	}
	return metric, true
}
