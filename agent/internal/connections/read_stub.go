//go:build !linux

package connections

// Read returns empty counts on non-Linux platforms.
func Read() Tracker {
	return Tracker{}
}
