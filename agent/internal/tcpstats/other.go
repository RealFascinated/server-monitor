//go:build !linux

package tcpstats

// Read reports nothing on platforms without the Linux /proc counters.
func Read() Snapshot {
	return Snapshot{}
}
