package ingest

import (
	"fmt"
	"log/slog"
	"strconv"
	"strings"
	"time"
)

const (
	defaultBufferMaxAge   = 6 * time.Hour
	defaultBufferMaxBytes = 16 << 20
	// maxBackfillAge is the oldest sample the API will accept, so holding anything older is pointless.
	maxBackfillAge = 6 * time.Hour
)

// BufferConfig controls how samples are held while the API is unreachable.
type BufferConfig struct {
	Enabled    bool
	MaxAge     time.Duration
	MaxBytes   int
	MaxSamples int // 0 = bounded by age and bytes only
}

func (b BufferConfig) Limits() Limits {
	return Limits{MaxAge: b.MaxAge, MaxBytes: b.MaxBytes, MaxSamples: b.MaxSamples}
}

func resolveBufferConfig(raw fileConfig) (BufferConfig, error) {
	buffer := BufferConfig{
		Enabled:  boolOrDefault(raw.EnablePushBuffer, true),
		MaxAge:   defaultBufferMaxAge,
		MaxBytes: defaultBufferMaxBytes,
	}

	if value := strings.TrimSpace(raw.BufferMaxAge); value != "" {
		age, err := time.ParseDuration(value)
		if err != nil || age <= 0 {
			return BufferConfig{}, fmt.Errorf("buffer_max_age %q must be a positive duration such as 30m or 6h", value)
		}
		if age > maxBackfillAge {
			slog.Warn("buffer_max_age exceeds what the API accepts, using the maximum",
				"configured", age, "used", maxBackfillAge)
			age = maxBackfillAge
		}
		buffer.MaxAge = age
	}

	if value := strings.TrimSpace(raw.BufferMaxBytes); value != "" {
		size, err := parseByteSize(value)
		if err != nil || size <= 0 {
			return BufferConfig{}, fmt.Errorf("buffer_max_bytes %q must be a positive size such as 16MiB", value)
		}
		buffer.MaxBytes = size
	}

	if value := strings.TrimSpace(raw.BufferMaxSamples); value != "" {
		count, err := strconv.Atoi(value)
		if err != nil || count <= 0 {
			return BufferConfig{}, fmt.Errorf("buffer_max_samples %q must be a positive integer", value)
		}
		buffer.MaxSamples = count
	}
	return buffer, nil
}

// parseByteSize accepts a byte count with an optional k/m/g suffix (powers of 1024, "b"/"ib" allowed).
func parseByteSize(value string) (int, error) {
	lower := strings.ToLower(strings.TrimSpace(value))
	lower = strings.TrimSuffix(strings.TrimSuffix(lower, "b"), "i")

	multiplier := 1
	switch {
	case strings.HasSuffix(lower, "k"):
		multiplier, lower = 1<<10, strings.TrimSuffix(lower, "k")
	case strings.HasSuffix(lower, "m"):
		multiplier, lower = 1<<20, strings.TrimSuffix(lower, "m")
	case strings.HasSuffix(lower, "g"):
		multiplier, lower = 1<<30, strings.TrimSuffix(lower, "g")
	}

	number, err := strconv.Atoi(strings.TrimSpace(lower))
	if err != nil {
		return 0, err
	}
	if number > (1<<31-1)/multiplier {
		return 0, fmt.Errorf("size too large")
	}
	return number * multiplier, nil
}
