//go:build !linux && !windows

package fd

func read() Snapshot {
	return Snapshot{}
}
