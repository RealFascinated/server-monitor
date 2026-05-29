//go:build linux

package update

import (
	"log/slog"
	"os/exec"
)

func stopService() error {
	return nil
}

func restartService() error {
	systemctl, err := exec.LookPath("systemctl")
	if err != nil {
		return nil
	}

	service := ServiceName + ".service"
	if err := exec.Command(systemctl, "is-active", "--quiet", service).Run(); err != nil {
		return nil
	}

	slog.Info("restarting service", "service", service)
	return exec.Command(systemctl, "restart", service).Run()
}
