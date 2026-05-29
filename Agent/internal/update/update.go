package update

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"runtime"
	"strings"

	"github.com/blang/semver/v4"
)

const (
	GitHubOwner = "RealFascinated"
	GitHubRepo  = "Monitor-API"
	BinaryName  = "monitor-agent"
	ServiceName = "monitor-agent"
	TagPrefix   = "agent/v"
)

// Run checks GitHub for a newer agent release, applies it, and restarts the service when needed.
func Run(currentVersion string) (bool, error) {
	if runtime.GOOS != "linux" && runtime.GOOS != "windows" {
		return false, fmt.Errorf("updates are only supported on linux and windows")
	}

	current, err := parseVersion(currentVersion)
	if err != nil {
		return false, fmt.Errorf("parse current version: %w", err)
	}

	slog.Info("checking for updates", "current", current.String())

	latestRelease, err := fetchLatestAgentRelease()
	if err != nil {
		return false, err
	}

	latest, err := parseVersion(strings.TrimPrefix(latestRelease.TagName, TagPrefix))
	if err != nil {
		return false, fmt.Errorf("parse latest version: %w", err)
	}

	if !latest.GT(current) {
		slog.Info("already up to date", "version", current.String())
		return false, nil
	}

	assetName := releaseAssetName(runtime.GOOS, runtime.GOARCH)
	asset, err := latestRelease.findAsset(assetName)
	if err != nil {
		return false, err
	}

	workDir, err := os.MkdirTemp("", "monitor-agent-update-*")
	if err != nil {
		return false, err
	}
	defer os.RemoveAll(workDir)

	slog.Info("downloading update", "version", latest.String(), "asset", asset.Name)

	downloadPath := filepath.Join(workDir, asset.Name)
	if err := downloadFile(asset.BrowserDownloadURL, downloadPath); err != nil {
		return false, err
	}

	if err := verifyChecksum(latestRelease, asset.Name, downloadPath); err != nil {
		return false, err
	}

	slog.Info("installing update", "version", latest.String())

	if err := installBinary(downloadPath); err != nil {
		return false, err
	}

	if err := restartService(); err != nil {
		slog.Warn("restart service", "err", err)
		slog.Warn("update installed; restart the service manually if it is still running")
	}

	slog.Info("update completed successfully", "version", latest.String())
	return true, nil
}

func parseVersion(raw string) (semver.Version, error) {
	raw = strings.TrimSpace(strings.TrimPrefix(raw, "v"))
	if raw == "" {
		return semver.Version{}, fmt.Errorf("empty version")
	}
	return semver.Parse(raw)
}

func releaseAssetName(goos, goarch string) string {
	name := fmt.Sprintf("%s-%s-%s", BinaryName, goos, goarch)
	if goos == "windows" {
		name += ".exe"
	}
	return name
}
