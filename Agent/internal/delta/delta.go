package delta

// Uint64 returns the non-negative difference between current and previous counters.
// Handles counter resets by returning current when it wraps backward.
func Uint64(current, previous uint64) uint64 {
	if current >= previous {
		return current - previous
	}
	return current
}
