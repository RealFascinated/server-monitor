package ingest

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

const (
	pushTimeout   = 30 * time.Second
	maxPushBody   = 4096
	maxPushTries  = 3
	retryBackoff  = 2 * time.Second
)

var httpClient = &http.Client{Timeout: pushTimeout}

func Push(config *Config, data Data, agentVersion string) error {
	body, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("marshal ingest data: %w", err)
	}

	var lastErr error
	for attempt := 1; attempt <= maxPushTries; attempt++ {
		lastErr = pushOnce(config, body, agentVersion)
		if lastErr == nil {
			return nil
		}
		if attempt == maxPushTries || !shouldRetry(lastErr) {
			return lastErr
		}
		time.Sleep(retryBackoff)
	}
	return lastErr
}

func pushOnce(config *Config, body []byte, agentVersion string) error {
	req, err := http.NewRequest(http.MethodPost, config.ApiEndpoint, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("create ingest request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+config.IngestToken)
	if agentVersion != "" {
		req.Header.Set("User-Agent", "monitor-agent/"+agentVersion)
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("push ingest data: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= http.StatusBadRequest {
		responseBody, _ := io.ReadAll(io.LimitReader(resp.Body, maxPushBody))
		return &httpStatusError{status: resp.StatusCode, body: bytes.TrimSpace(responseBody)}
	}

	return nil
}

type httpStatusError struct {
	status int
	body   []byte
}

func (e *httpStatusError) Error() string {
	if len(e.body) == 0 {
		return fmt.Sprintf("push ingest data: status %d", e.status)
	}
	return fmt.Sprintf("push ingest data: status %d: %s", e.status, e.body)
}

func shouldRetry(err error) bool {
	statusErr, ok := err.(*httpStatusError)
	if !ok {
		return true
	}
	return statusErr.status >= http.StatusInternalServerError
}
