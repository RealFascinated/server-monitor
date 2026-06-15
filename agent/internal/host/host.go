package host

import (
	"net"
	"strings"
	"sync"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/zfs"
)

const ipCacheTTL = 5 * time.Minute

var (
	ipCacheMu  sync.Mutex
	cachedIP   string
	ipCachedAt time.Time
)

func GetIP() string {
	ipCacheMu.Lock()
	if cachedIP != "" && time.Since(ipCachedAt) < ipCacheTTL {
		ip := cachedIP
		ipCacheMu.Unlock()
		return ip
	}
	ipCacheMu.Unlock()

	ip := discoverIP()

	ipCacheMu.Lock()
	cachedIP = ip
	ipCachedAt = time.Now()
	ipCacheMu.Unlock()
	return ip
}

func discoverIP() string {
	conn, err := net.Dial("udp4", "1.1.1.1:53")
	if err == nil {
		defer conn.Close()
		if addr, ok := conn.LocalAddr().(*net.UDPAddr); ok && addr.IP != nil && !addr.IP.IsLoopback() {
			return addr.IP.String()
		}
	}

	if addrs, err := net.InterfaceAddrs(); err == nil {
		for _, addr := range addrs {
			if ipNet, ok := addr.(*net.IPNet); ok && ipNet.IP.To4() != nil && !ipNet.IP.IsLoopback() {
				return ipNet.IP.String()
			}
		}
	}

	return "127.0.0.1"
}

func PopulateDetails() (ingest.ServerDetails, bool) {
	details := ingest.ServerDetails{Ip: GetIP()}

	if uptime, err := readUptime(); err == nil {
		details.UptimeSeconds = int(uptime)
	}

	details = populatePlatformDetails(details)
	return details, zfs.Available()
}

func ReadOSReleaseValue(data, key string) string {
	prefix := key + "="
	for line := range strings.SplitSeq(data, "\n") {
		if strings.HasPrefix(line, prefix) {
			value := strings.TrimPrefix(line, prefix)
			value = strings.Trim(value, "\"")
			return value
		}
	}
	return ""
}

func UptimeSeconds() (int, error) {
	uptime, err := readUptime()
	return int(uptime), err
}
