package ingest

import (
	"net/url"
	"strings"
)

const (
	defaultHeartbeatEndpoint   = "https://monitor.fascinated.cc/api/v1/servers/heartbeat"
	defaultAgentUpdateEndpoint = "https://monitor.fascinated.cc/api/v1/agent/update"
)

func (c *Config) HeartbeatURL() string {
	return siblingEndpoint(c.ApiEndpoint, "heartbeat", defaultHeartbeatEndpoint)
}

func (c *Config) UpdateURL() string {
	return agentUpdateURL(c.ApiEndpoint)
}

func agentUpdateURL(apiEndpoint string) string {
	trimmed := strings.TrimSpace(apiEndpoint)
	if trimmed == "" {
		return defaultAgentUpdateEndpoint
	}

	u, err := url.Parse(trimmed)
	if err != nil {
		return defaultAgentUpdateEndpoint
	}

	if i := strings.Index(u.Path, "/v1/"); i >= 0 {
		u.Path = u.Path[:i] + "/v1/agent/update"
	} else {
		u.Path = strings.TrimSuffix(u.Path, "/") + "/v1/agent/update"
	}
	return u.String()
}

func siblingEndpoint(apiEndpoint, sibling, fallback string) string {
	trimmed := strings.TrimSpace(apiEndpoint)
	if trimmed == "" {
		return fallback
	}
	if strings.HasSuffix(trimmed, "/ingest") {
		return strings.TrimSuffix(trimmed, "/ingest") + "/" + sibling
	}

	u, err := url.Parse(trimmed)
	if err != nil || u.Path == "" || u.Path == "/" {
		return fallback
	}

	path := strings.TrimSuffix(u.Path, "/")
	if i := strings.LastIndex(path, "/"); i >= 0 {
		u.Path = path[:i+1] + sibling
	} else {
		u.Path = "/" + sibling
	}
	return u.String()
}
