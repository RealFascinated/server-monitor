//go:build !linux && !windows

package collector

func collect(opts Options) (Result, error) {
	return collectGopsutil(opts, gopsutilOptions{
		enableIowaitSample: false,
	})
}
