//go:build linux

package thermal

import (
	"path/filepath"
	"strings"
)

// hwmonDeviceID returns a stable per-device id (e.g. nvme0, sda) for generic hwmon chip names.
func hwmonDeviceID(hwmonDir string) string {
	root := hwmonRootDir(hwmonDir)
	if id := deviceIDFromPath(root); isDeviceScopedID(id, root) {
		return id
	}
	link := filepath.Join(root, "device")
	target, err := filepath.EvalSymlinks(link)
	if err != nil {
		return filepath.Base(root)
	}
	if id := deviceIDFromPath(target); id != "" {
		return id
	}
	return filepath.Base(root)
}

func isDeviceScopedID(id, hwmonRoot string) bool {
	if id == "" || id == filepath.Base(hwmonRoot) {
		return false
	}
	return !strings.HasPrefix(id, "hwmon")
}

func hwmonRootDir(dir string) string {
	if filepath.Base(dir) == "device" {
		return filepath.Dir(dir)
	}
	return dir
}

func nvmeControllerID(hwmonDir string) string {
	root := hwmonRootDir(hwmonDir)
	target, err := filepath.EvalSymlinks(filepath.Join(root, "device"))
	if err != nil {
		return ""
	}
	return nvmeControllerFromPath(target)
}

func nvmeControllerFromPath(devicePath string) string {
	parts := strings.Split(strings.Trim(filepath.ToSlash(devicePath), "/"), "/")
	for i := len(parts) - 1; i >= 0; i-- {
		if isNVMeController(parts[i]) {
			return parts[i]
		}
	}
	return ""
}

func isNVMeController(part string) bool {
	if part == "nvme" || !strings.HasPrefix(part, "nvme") {
		return false
	}
	suffix := part[4:]
	if suffix == "" {
		return false
	}
	for i := 0; i < len(suffix); i++ {
		if suffix[i] < '0' || suffix[i] > '9' {
			return false
		}
	}
	return true
}

func deviceIDFromPath(devicePath string) string {
	if id := nvmeControllerFromPath(devicePath); id != "" {
		return id
	}
	parts := strings.Split(strings.Trim(filepath.ToSlash(devicePath), "/"), "/")
	for i := len(parts) - 1; i >= 0; i-- {
		if isBlockDeviceName(parts[i]) {
			return parts[i]
		}
	}
	if pci := pciAddressFromPath(parts); pci != "" {
		return "pci-" + pci
	}
	if len(parts) > 0 {
		return parts[len(parts)-1]
	}
	return ""
}

func isBlockDeviceName(name string) bool {
	if strings.HasPrefix(name, "sd") || strings.HasPrefix(name, "hd") {
		return len(name) >= 3
	}
	if !strings.HasPrefix(name, "nvme") || len(name) < 7 {
		return false
	}
	// nvme0n1 namespace device only (not nvme0 controller or nvme1 controller).
	suffix := name[4:]
	for i := 0; i < len(suffix); i++ {
		if suffix[i] == 'n' {
			return i > 0 && i < len(suffix)-1
		}
		if suffix[i] < '0' || suffix[i] > '9' {
			return false
		}
	}
	return false
}

func pciAddressFromPath(parts []string) string {
	for _, part := range parts {
		if strings.Count(part, ":") < 2 {
			continue
		}
		return strings.TrimPrefix(part, "0000:")
	}
	return ""
}

func usesDeviceScopedHwmonName(hwName string) bool {
	switch hwName {
	case "nvme", "drivetemp":
		return true
	default:
		return false
	}
}
