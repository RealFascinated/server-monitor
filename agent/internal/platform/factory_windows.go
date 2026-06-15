//go:build windows

package platform

func New(opts Options) Backend {
	return newWindowsBackend(opts)
}
