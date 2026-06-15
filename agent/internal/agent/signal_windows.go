//go:build !unix

package agent

import "os"

func reloadSignal() <-chan os.Signal {
	return make(chan os.Signal)
}
