//go:build linux

package platform

func New(opts Options) Backend {
	return newLinuxBackend(opts)
}
