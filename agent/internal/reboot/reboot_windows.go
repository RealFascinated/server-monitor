//go:build windows

package reboot

import "golang.org/x/sys/windows/registry"

// keys are the registry markers Windows leaves behind when an installed update is waiting for
// a restart: Windows Update's own flag and the servicing stack's.
var keys = []string{
	`SOFTWARE\Microsoft\Windows\CurrentVersion\WindowsUpdate\Auto Update\RebootRequired`,
	`SOFTWARE\Microsoft\Windows\CurrentVersion\Component Based Servicing\RebootPending`,
}

// Required reports whether either reboot-pending key exists under HKLM.
func Required() bool {
	for _, path := range keys {
		key, err := registry.OpenKey(registry.LOCAL_MACHINE, path, registry.QUERY_VALUE)
		if err != nil {
			continue
		}
		key.Close()
		return true
	}
	return false
}
