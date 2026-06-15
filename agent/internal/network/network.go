package network

import (
	"regexp"
	"runtime"
	"strings"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
)

const maxBytesPerSecond = 10_000_000_000

var commonLinuxInterfacePattern = regexp.MustCompile(`^(eth[0-9]+|ens[0-9]+|enp[0-9]+s[0-9]+(d[0-9]+)?(f[0-9]+)?|eno[0-9]+|enx[0-9a-f]+|em[0-9]+|wlan[0-9]+|wlp[0-9]+s[0-9]+|bond[0-9]+|nic[0-9]+|vmbr[0-9]+)$`)

var excludedLinuxInterfacePrefixes = []string{
	"veth", "fwbr", "docker", "br-", "virbr", "tap", "tun", "wg", "dummy",
	"nlmon", "ifb", "vnet", "lxc", "tailscale", "pterodactyl",
}

func ComputeMetrics(before, after []Counter, elapsed time.Duration) []ingest.InterfaceMetrics {
	prev := make(map[string]Counter, len(before))
	for _, counter := range before {
		if !IsCommonInterface(counter.Name) {
			continue
		}
		prev[NormalizeInterface(counter.Name)] = counter
	}

	metrics := make([]ingest.InterfaceMetrics, 0, len(after))
	for _, counter := range after {
		name := NormalizeInterface(counter.Name)
		if !IsCommonInterface(counter.Name) {
			continue
		}
		previous, ok := prev[name]
		if !ok {
			continue
		}

		rxBps := iostats.PerSecond(delta.Uint64(counter.BytesRecv, previous.BytesRecv), elapsed)
		txBps := iostats.PerSecond(delta.Uint64(counter.BytesSent, previous.BytesSent), elapsed)
		if rxBps > maxBytesPerSecond || txBps > maxBytesPerSecond {
			continue
		}

		metrics = append(metrics, ingest.InterfaceMetrics{
			InterfaceName:       name,
			RxBytesPerSecond:    rxBps,
			TxBytesPerSecond:    txBps,
			RxPacketsPerSecond:  iostats.PerSecond(delta.Uint64(counter.PacketsRecv, previous.PacketsRecv), elapsed),
			TxPacketsPerSecond:  iostats.PerSecond(delta.Uint64(counter.PacketsSent, previous.PacketsSent), elapsed),
			RxErrorsPerSecond:   iostats.PerSecond(delta.Uint64(counter.Errin, previous.Errin), elapsed),
			TxErrorsPerSecond:   iostats.PerSecond(delta.Uint64(counter.Errout, previous.Errout), elapsed),
		})
	}
	return metrics
}

func NormalizeInterface(name string) string {
	if i := strings.Index(name, "@"); i >= 0 {
		return name[:i]
	}
	return name
}

func IsCommonInterface(name string) bool {
	name = NormalizeInterface(name)
	if name == "" || name == "lo" {
		return false
	}

	switch runtime.GOOS {
	case "linux":
		return isCommonLinuxInterface(name)
	case "windows":
		return isCommonWindowsInterface(name)
	default:
		return !hasExcludedPrefix(name, excludedLinuxInterfacePrefixes)
	}
}

func isCommonLinuxInterface(name string) bool {
	if hasExcludedPrefix(name, excludedLinuxInterfacePrefixes) {
		return false
	}
	return commonLinuxInterfacePattern.MatchString(name)
}

func isCommonWindowsInterface(name string) bool {
	lower := strings.ToLower(name)
	if strings.Contains(lower, "loopback") {
		return false
	}

	excluded := []string{
		"isatap", "teredo", "vethernet", "vmware", "virtualbox",
		"hyper-v", "bluetooth network", "npcap", "wintun",
	}
	for _, prefix := range excluded {
		if strings.Contains(lower, prefix) {
			return false
		}
	}
	return true
}

func hasExcludedPrefix(name string, prefixes []string) bool {
	for _, prefix := range prefixes {
		if strings.HasPrefix(name, prefix) {
			return true
		}
	}
	return false
}
