package platform

import (
	"log/slog"
	"os"
	"time"
)

var profileCollect = os.Getenv("MONITOR_PROFILE_COLLECT") == "1"

func profilePhase(phase string, start time.Time) {
	if !profileCollect {
		return
	}
	slog.Debug("collect profile", "phase", phase, "duration", time.Since(start).Round(time.Millisecond))
}
