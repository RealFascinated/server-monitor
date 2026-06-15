package ingest

import (
	"crypto/sha256"
	"encoding/hex"
	"strings"
)

// HashDeviceID returns a stable 16-character hex id from a platform device identifier.
func HashDeviceID(raw string) string {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return ""
	}
	sum := sha256.Sum256([]byte(strings.ToLower(raw)))
	return hex.EncodeToString(sum[:8])
}
