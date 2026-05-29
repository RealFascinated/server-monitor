//go:build unix

package agent

import (
	"os"
	"os/signal"
	"syscall"
)

func reloadSignal() <-chan os.Signal {
	ch := make(chan os.Signal, 1)
	signal.Notify(ch, syscall.SIGHUP)
	return ch
}
