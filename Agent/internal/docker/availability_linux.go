//go:build linux

package docker

import (
	"os/exec"
	"sync"
	"time"
)

const dockerRecheckInterval = 5 * time.Minute

var dockerState struct {
	mu               sync.Mutex
	checked          bool
	available        bool
	unavailableUntil time.Time
}

func dockerEnabled() bool {
	dockerState.mu.Lock()
	defer dockerState.mu.Unlock()

	now := time.Now()
	if dockerState.checked && !dockerState.available && now.Before(dockerState.unavailableUntil) {
		return false
	}

	if _, err := exec.LookPath("docker"); err != nil {
		dockerState.checked = true
		dockerState.available = false
		dockerState.unavailableUntil = now.Add(dockerRecheckInterval)
		return false
	}
	if err := exec.Command("docker", "info").Run(); err != nil {
		dockerState.checked = true
		dockerState.available = false
		dockerState.unavailableUntil = now.Add(dockerRecheckInterval)
		return false
	}

	dockerState.checked = true
	dockerState.available = true
	dockerState.unavailableUntil = time.Time{}
	return true
}
