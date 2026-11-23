//go:build linux

package tcpstats

import (
	"bufio"
	"io"
	"os"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

const (
	snmpPath       = "/proc/net/snmp"
	netstatPath    = "/proc/net/netstat"
	conntrackDir   = "/proc/sys/net/netfilter/"
	conntrackCount = conntrackDir + "nf_conntrack_count"
	conntrackLimit = conntrackDir + "nf_conntrack_max"
)

// Read samples the TCP counters and conntrack gauges. Missing files leave their fields nil.
func Read() Snapshot {
	var snap Snapshot

	if file, err := os.Open(linux.HostPath(snmpPath)); err == nil {
		if columns, ok := parseNetColumns(file, "Tcp", "RetransSegs"); ok {
			if value, found := columns["RetransSegs"]; found {
				snap.RetransSegs = uint64Ptr(value)
			}
		}
		file.Close()
	}

	if file, err := os.Open(linux.HostPath(netstatPath)); err == nil {
		if columns, ok := parseNetColumns(file, "TcpExt", "ListenOverflows", "ListenDrops"); ok {
			if value, found := columns["ListenOverflows"]; found {
				snap.ListenOverflows = uint64Ptr(value)
			}
			if value, found := columns["ListenDrops"]; found {
				snap.ListenDrops = uint64Ptr(value)
			}
		}
		file.Close()
	}

	entries, hasEntries := readInt64File(conntrackCount)
	limit, hasLimit := readInt64File(conntrackLimit)
	if hasEntries {
		snap.ConntrackEntries = int64Ptr(entries)
	}
	if hasLimit {
		snap.ConntrackMax = int64Ptr(limit)
	}
	if hasEntries && hasLimit && limit > 0 {
		snap.ConntrackUsagePercent = float64Ptr(float64(entries) / float64(limit) * 100)
	}

	return snap
}

// parseNetColumns reads the paired header and value lines of one table in /proc/net/snmp or
// /proc/net/netstat (e.g. "Tcp:" or "TcpExt:") and returns the named columns. The header names
// the fields, so column order differences across kernels do not matter.
func parseNetColumns(r io.Reader, table string, names ...string) (map[string]uint64, bool) {
	wanted := make(map[string]struct{}, len(names))
	for _, name := range names {
		wanted[name] = struct{}{}
	}

	scanner := bufio.NewScanner(r)
	var header []string
	headerSet := false
	for scanner.Scan() {
		key, rest, ok := strings.Cut(scanner.Text(), ":")
		if !ok || strings.TrimSpace(key) != table {
			continue
		}
		fields := strings.Fields(rest)
		if !headerSet {
			header = fields
			headerSet = true
			continue
		}

		columns := make(map[string]uint64, len(wanted))
		for i, name := range header {
			if _, ok := wanted[name]; !ok || i >= len(fields) {
				continue
			}
			value, err := strconv.ParseUint(fields[i], 10, 64)
			if err != nil {
				continue
			}
			columns[name] = value
		}
		return columns, len(columns) > 0
	}
	return nil, false
}

func readInt64File(path string) (int64, bool) {
	data, err := os.ReadFile(linux.HostPath(path))
	if err != nil {
		return 0, false
	}
	value, err := strconv.ParseInt(strings.TrimSpace(string(data)), 10, 64)
	if err != nil {
		return 0, false
	}
	return value, true
}

func uint64Ptr(v uint64) *uint64 { return &v }
