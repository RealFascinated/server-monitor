//go:build linux

package connections

import (
	"bufio"
	"io"
	"os"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

var tcpStateNames = map[uint8]string{
	0x01: "ESTABLISHED",
	0x02: "SYN_SENT",
	0x03: "SYN_RECV",
	0x04: "FIN_WAIT1",
	0x05: "FIN_WAIT2",
	0x06: "TIME_WAIT",
	0x07: "CLOSE",
	0x08: "CLOSE_WAIT",
	0x09: "LAST_ACK",
	0x0A: "LISTEN",
	0x0B: "CLOSING",
}

// Read counts TCP sockets by state from /proc/net/tcp and /proc/net/tcp6.
func Read() Tracker {
	counts := make(map[string]int64)
	readTCPFile(linux.HostPath("/proc/net/tcp"), counts)
	readTCPFile(linux.HostPath("/proc/net/tcp6"), counts)
	if len(counts) == 0 {
		return Tracker{}
	}
	return Tracker{States: counts}
}

func readTCPFile(path string, counts map[string]int64) {
	file, err := os.Open(path)
	if err != nil {
		return
	}
	defer file.Close()
	parseTCPTable(file, counts)
}

func parseTCPTable(r io.Reader, counts map[string]int64) {
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		line := scanner.Text()
		if line == "" || strings.HasPrefix(line, "sl") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 4 {
			continue
		}
		stateHex := fields[3]
		if len(stateHex) < 2 {
			continue
		}
		// Kernel prints st as hex without 0x prefix (e.g. 01, 0A).
		value, err := strconv.ParseUint(stateHex, 16, 8)
		if err != nil {
			continue
		}
		name, ok := tcpStateNames[uint8(value)]
		if !ok {
			name = "UNKNOWN_" + strings.ToUpper(stateHex)
		}
		counts[name]++
	}
}
