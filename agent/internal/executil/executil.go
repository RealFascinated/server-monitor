package executil

import (
	"context"
	"os/exec"
	"time"
)

const DefaultTimeout = 5 * time.Second

func CommandOutput(name string, args ...string) ([]byte, error) {
	return CommandOutputTimeout(DefaultTimeout, name, args...)
}

// CommandOutputTimeout is CommandOutput with a caller-chosen limit, for tools that can be slow.
// Like exec.Cmd.Output it returns whatever the command printed even when it exits non-zero.
func CommandOutputTimeout(timeout time.Duration, name string, args ...string) ([]byte, error) {
	ctx, cancel := context.WithTimeout(context.Background(), timeout)
	defer cancel()
	return exec.CommandContext(ctx, name, args...).Output()
}

func LookPath(name string) (string, error) {
	return exec.LookPath(name)
}
