package timesync

import (
	"regexp"
	"strconv"
	"strings"
)

// parseChronyTracking reads `chronyc -c tracking`. It returns the offset of the system clock from
// the reference in milliseconds, positive when the clock is ahead, and false when chrony has no
// reference to compare against.
func parseChronyTracking(out []byte) (float64, bool) {
	fields := strings.Split(strings.TrimSpace(string(out)), ",")
	// refid, name, stratum, ref time, system time, last offset, ...
	if len(fields) < 5 {
		return 0, false
	}
	if stratum, err := strconv.Atoi(fields[2]); err != nil || stratum == 0 {
		return 0, false
	}
	systemTime, err := strconv.ParseFloat(fields[4], 64)
	if err != nil {
		return 0, false
	}
	// chrony reports how much it still has to correct: positive means the clock is slow.
	return -systemTime * 1000, true
}

var w32tmPhaseOffset = regexp.MustCompile(`(?m)^\s*Phase Offset:\s*(-?[0-9.]+)s`)

// parseW32tmStatus reads `w32tm /query /status`. Either result is nil when the output does not say.
func parseW32tmStatus(out string) (synced *bool, offsetMs *float64) {
	for _, line := range strings.Split(out, "\n") {
		key, value, found := strings.Cut(line, ":")
		if !found || strings.TrimSpace(key) != "Source" {
			continue
		}
		source := strings.ToLower(strings.TrimSpace(value))
		if source == "" {
			break
		}
		unsynced := strings.Contains(source, "local cmos clock") || strings.Contains(source, "free-running")
		synced = boolPtr(!unsynced)
		break
	}

	if match := w32tmPhaseOffset.FindStringSubmatch(out); match != nil {
		if seconds, err := strconv.ParseFloat(match[1], 64); err == nil {
			offsetMs = floatPtr(seconds * 1000)
		}
	}
	return synced, offsetMs
}
