//go:build !linux

package psi

import "fascinated.cc/monitor/agent/internal/ingest"

func read() *ingest.PsiMetrics {
	return nil
}
