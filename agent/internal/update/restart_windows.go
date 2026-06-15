//go:build windows

package update

import (
	"log/slog"
	"os/exec"
)

func stopService() error {
	slog.Info("stopping service", "service", ServiceName)
	return exec.Command("sc", "stop", ServiceName).Run()
}

func restartService() error {
	slog.Info("starting service", "service", ServiceName)
	return exec.Command("sc", "start", ServiceName).Run()
}
