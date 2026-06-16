//go:build !linux && !windows

package battery

func read() Snapshot {
	return Snapshot{}
}
