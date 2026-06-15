package update

import (
	"crypto/sha256"
	"encoding/hex"
	"os"
	"path/filepath"
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

func TestVerifyFileChecksum(t *testing.T) {
	t.Parallel()

	dir := t.TempDir()
	path := filepath.Join(dir, "monitor-agent-linux-amd64")
	content := []byte("test-binary")
	if err := os.WriteFile(path, content, 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	sum := sha256.Sum256(content)
	expected := hex.EncodeToString(sum[:])
	if err := verifyFileChecksum(path, expected); err != nil {
		t.Fatalf("verifyFileChecksum() error = %v", err)
	}
	if err := verifyFileChecksum(path, "deadbeef"); err == nil {
		t.Fatal("expected checksum mismatch")
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
