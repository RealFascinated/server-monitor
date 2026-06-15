package executil

import (
	"context"
	"os/exec"
	"time"
)

const DefaultTimeout = 5 * time.Second

func CommandOutput(name string, args ...string) ([]byte, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DefaultTimeout)
	defer cancel()
	return exec.CommandContext(ctx, name, args...).Output()
}

func LookPath(name string) (string, error) {
	return exec.LookPath(name)
}
