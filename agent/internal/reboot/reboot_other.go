//go:build !linux && !windows

package reboot

// Required always reports false on platforms without a reboot flag.
func Required() bool { return false }
