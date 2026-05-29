package update

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

type releaseAsset struct {
	Name               string `json:"name"`
	BrowserDownloadURL string `json:"browser_download_url"`
}

type release struct {
	TagName string         `json:"tag_name"`
	Assets  []releaseAsset `json:"assets"`
}

func (r release) findAsset(name string) (releaseAsset, error) {
	for _, asset := range r.Assets {
		if asset.Name == name {
			return asset, nil
		}
	}
	return releaseAsset{}, fmt.Errorf("release %s is missing asset %s", r.TagName, name)
}

func fetchLatestAgentRelease() (release, error) {
	url := fmt.Sprintf("https://api.github.com/repos/%s/%s/releases?per_page=100", GitHubOwner, GitHubRepo)
	req, err := http.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		return release{}, err
	}
	req.Header.Set("Accept", "application/vnd.github+json")
	req.Header.Set("User-Agent", "monitor-agent-updater")

	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return release{}, err
	}
	defer res.Body.Close()

	body, err := io.ReadAll(res.Body)
	if err != nil {
		return release{}, err
	}
	if res.StatusCode >= 400 {
		return release{}, fmt.Errorf("fetch releases (%d): %s", res.StatusCode, strings.TrimSpace(string(body)))
	}

	var releases []release
	if err := json.Unmarshal(body, &releases); err != nil {
		return release{}, err
	}

	for _, item := range releases {
		if strings.HasPrefix(item.TagName, TagPrefix) {
			return item, nil
		}
	}

	return release{}, fmt.Errorf("no %s releases found", TagPrefix)
}

func downloadFile(url, destPath string) error {
	req, err := http.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		return err
	}
	req.Header.Set("User-Agent", "monitor-agent-updater")

	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()

	if res.StatusCode >= 400 {
		return fmt.Errorf("download %s (%d)", url, res.StatusCode)
	}

	if err := os.MkdirAll(filepath.Dir(destPath), 0o755); err != nil {
		return err
	}

	file, err := os.Create(destPath)
	if err != nil {
		return err
	}
	defer file.Close()

	if _, err := io.Copy(file, res.Body); err != nil {
		return err
	}

	return nil
}

func verifyChecksum(latest release, assetName, assetPath string) error {
	checksumsAsset, err := latest.findAsset("checksums.txt")
	if err != nil {
		return fmt.Errorf("verify checksum: %w", err)
	}

	workDir := filepath.Dir(assetPath)
	checksumsPath := filepath.Join(workDir, "checksums.txt")
	if err := downloadFile(checksumsAsset.BrowserDownloadURL, checksumsPath); err != nil {
		return fmt.Errorf("download checksums: %w", err)
	}

	data, err := os.ReadFile(checksumsPath)
	if err != nil {
		return err
	}

	expected, ok := parseChecksum(string(data), assetName)
	if !ok {
		return fmt.Errorf("checksum for %s not found", assetName)
	}

	file, err := os.Open(assetPath)
	if err != nil {
		return err
	}
	defer file.Close()

	hash := sha256.New()
	if _, err := io.Copy(hash, file); err != nil {
		return err
	}

	actual := hex.EncodeToString(hash.Sum(nil))
	if actual != expected {
		return fmt.Errorf("checksum mismatch for %s", assetName)
	}

	return nil
}

func parseChecksum(content, assetName string) (string, bool) {
	for line := range strings.SplitSeq(strings.TrimSpace(content), "\n") {
		fields := strings.Fields(line)
		if len(fields) != 2 || fields[1] != assetName {
			continue
		}
		return fields[0], true
	}
	return "", false
}
