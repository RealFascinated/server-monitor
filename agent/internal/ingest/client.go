package ingest

import (
	"bytes"
	"fmt"
	"io"
	"net/http"
	"time"
)

const (
	requestTimeout = 30 * time.Second
	maxErrorBody   = 4096
	maxRetries     = 3
	retryBackoff   = 2 * time.Second
)

var httpClient = &http.Client{Timeout: requestTimeout}

func postWithRetry(op, url, token, agentVersion string, body []byte) error {
	var lastErr error
	for attempt := 1; attempt <= maxRetries; attempt++ {
		lastErr = post(op, url, token, agentVersion, body)
		if lastErr == nil {
			return nil
		}
		if attempt == maxRetries || !shouldRetry(lastErr) {
			return lastErr
		}
		time.Sleep(retryBackoff)
	}
	return lastErr
}

func post(op, url, token, agentVersion string, body []byte) error {
	var bodyReader io.Reader
	if body != nil {
		bodyReader = bytes.NewReader(body)
	}

	req, err := http.NewRequest(http.MethodPost, url, bodyReader)
	if err != nil {
		return fmt.Errorf("%s: create request: %w", op, err)
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	req.Header.Set("Authorization", "Bearer "+token)
	if agentVersion != "" {
		req.Header.Set("User-Agent", "monitor-agent/"+agentVersion)
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("%s: %w", op, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= http.StatusBadRequest {
		responseBody, _ := io.ReadAll(io.LimitReader(resp.Body, maxErrorBody))
		return &httpStatusError{op: op, status: resp.StatusCode, body: bytes.TrimSpace(responseBody)}
	}

	return nil
}

type httpStatusError struct {
	op     string
	status int
	body   []byte
}

func (e *httpStatusError) Error() string {
	if len(e.body) == 0 {
		return fmt.Sprintf("%s: status %d", e.op, e.status)
	}
	return fmt.Sprintf("%s: status %d: %s", e.op, e.status, e.body)
}

func shouldRetry(err error) bool {
	statusErr, ok := err.(*httpStatusError)
	if !ok {
		return true
	}
	return statusErr.status >= http.StatusInternalServerError
}
