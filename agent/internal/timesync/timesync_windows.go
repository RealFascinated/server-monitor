//go:build windows

package timesync

import "fascinated.cc/monitor/agent/internal/executil"

func read() Status {
	out, err := executil.CommandOutput("w32tm", "/query", "/status")
	if err != nil {
		return Status{}
	}
	synced, offset := parseW32tmStatus(string(out))
	return Status{Synced: synced, OffsetMs: offset}
}
