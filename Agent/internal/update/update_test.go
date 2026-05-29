package update

import (
	"testing"

	"github.com/blang/semver/v4"
)

func TestReleaseAssetName(t *testing.T) {
	t.Parallel()

	tests := []struct {
		goos, goarch, want string
	}{
		{"linux", "amd64", "monitor-agent-linux-amd64"},
		{"linux", "arm64", "monitor-agent-linux-arm64"},
		{"windows", "amd64", "monitor-agent-windows-amd64.exe"},
	}

	for _, tc := range tests {
		if got := releaseAssetName(tc.goos, tc.goarch); got != tc.want {
			t.Fatalf("releaseAssetName(%q, %q) = %q, want %q", tc.goos, tc.goarch, got, tc.want)
		}
	}
}

func TestParseChecksum(t *testing.T) {
	t.Parallel()

	content := "abc123  monitor-agent-linux-amd64\n" +
		"def456  checksums.txt\n"

	got, ok := parseChecksum(content, "monitor-agent-linux-amd64")
	if !ok {
		t.Fatal("expected checksum to be found")
	}
	if got != "abc123" {
		t.Fatalf("parseChecksum() = %q, want abc123", got)
	}
}

func TestParseVersion(t *testing.T) {
	t.Parallel()

	version, err := parseVersion("2.1.0")
	if err != nil {
		t.Fatalf("parseVersion() error = %v", err)
	}
	if version.String() != "2.1.0" {
		t.Fatalf("parseVersion() = %q, want 2.1.0", version.String())
	}
}

func TestVersionComparison(t *testing.T) {
	t.Parallel()

	current := semver.MustParse("2.0.0")
	latest := semver.MustParse("2.0.1")

	if !latest.GT(current) {
		t.Fatal("expected latest version to be greater than current")
	}
}
