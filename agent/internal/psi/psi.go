// Package psi reads Linux Pressure Stall Information (PSI) from /proc/pressure.
package psi

import "fascinated.cc/monitor/agent/internal/ingest"

// Read returns stall pressure for this host, or nil when the kernel does not expose PSI.
func Read() *ingest.PsiMetrics {
	return read()
}
