package update

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"runtime"
	"strings"

	"fascinated.cc/monitor/agent/internal/ingest"
	"github.com/blang/semver/v4"
)

const (
	BinaryName  = "monitor-agent"
	ServiceName = "monitor-agent"
)

// Run checks the API for a newer agent release, applies it, and restarts the service when needed.
func Run(currentVersion string) (bool, error) {
	if runtime.GOOS != "linux" && runtime.GOOS != "windows" {
		return false, fmt.Errorf("updates are only supported on linux and windows")
	}

	config, err := ingest.LoadConfigForPrint()
	if err != nil {
		return false, fmt.Errorf("load config: %w", err)
	}

	current, err := parseVersion(currentVersion)
	if err != nil {
		return false, fmt.Errorf("parse current version: %w", err)
	}

	slog.Info("checking for updates", "current", current.String())

	updateInfo, err := fetchUpdate(config.UpdateURL(), current.String())
	if err != nil {
		return false, err
	}
	if updateInfo == nil {
		slog.Info("already up to date", "version", current.String())
		return false, nil
	}

	latest, err := parseVersion(updateInfo.Version)
	if err != nil {
		return false, fmt.Errorf("parse latest version: %w", err)
	}

	assetName := releaseAssetName(runtime.GOOS, runtime.GOARCH)
	asset, err := findAsset(updateInfo.Assets, assetName)
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
	if err := downloadFile(asset.DownloadURL, downloadPath); err != nil {
		return false, err
	}

	if err := verifyFileChecksum(downloadPath, asset.Checksum); err != nil {
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

func findAsset(assets []updateAsset, name string) (updateAsset, error) {
	for _, asset := range assets {
		if asset.Name == name {
			return asset, nil
		}
	}
	return updateAsset{}, fmt.Errorf("update is missing asset %s", name)
}
