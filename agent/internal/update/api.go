package update

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
)

type updateAsset struct {
	Name        string `json:"name"`
	DownloadURL string `json:"downloadUrl"`
	Checksum    string `json:"checksum"`
}

type updateResponse struct {
	Version string        `json:"version"`
	Assets  []updateAsset `json:"assets"`
}

func fetchUpdate(apiURL, currentVersion string) (*updateResponse, error) {
	endpoint, err := url.Parse(apiURL)
	if err != nil {
		return nil, fmt.Errorf("parse update url: %w", err)
	}

	query := endpoint.Query()
	query.Set("version", currentVersion)
	endpoint.RawQuery = query.Encode()

	req, err := http.NewRequest(http.MethodGet, endpoint.String(), nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "monitor-agent-updater")

	res, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode == http.StatusNoContent {
		return nil, nil
	}

	body, err := io.ReadAll(res.Body)
	if err != nil {
		return nil, err
	}
	if res.StatusCode >= 400 {
		return nil, fmt.Errorf("check update (%d): %s", res.StatusCode, strings.TrimSpace(string(body)))
	}

	var response updateResponse
	if err := json.Unmarshal(body, &response); err != nil {
		return nil, err
	}
	if strings.TrimSpace(response.Version) == "" {
		return nil, fmt.Errorf("update response missing version")
	}
	if len(response.Assets) == 0 {
		return nil, fmt.Errorf("update response missing assets")
	}
	return &response, nil
}
