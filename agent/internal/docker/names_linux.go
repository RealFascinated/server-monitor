//go:build linux

package docker

import (
	"context"
	"encoding/json"
	"io"
	"log/slog"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

const (
	dockerSocketPath = "/var/run/docker.sock"
	namesCacheTTL    = 5 * time.Minute
)

type containerNameEntry struct {
	name string
	at   time.Time
}

var namesCache struct {
	mu      sync.Mutex
	entries map[string]containerNameEntry
}

func resolveContainerName(id string) string {
	if id == "" {
		return ""
	}
	if name := cachedContainerName(id); name != "" {
		return name
	}
	names := fetchContainerNames()
	namesCache.mu.Lock()
	defer namesCache.mu.Unlock()
	if namesCache.entries == nil {
		namesCache.entries = make(map[string]containerNameEntry)
	}
	now := time.Now()
	for cid, name := range names {
		namesCache.entries[cid] = containerNameEntry{name: name, at: now}
	}
	if name, ok := names[id]; ok {
		return name
	}
	if len(id) > 12 {
		return id[:12]
	}
	return id
}

func cachedContainerName(id string) string {
	namesCache.mu.Lock()
	defer namesCache.mu.Unlock()
	if entry, ok := namesCache.entries[id]; ok && time.Since(entry.at) < namesCacheTTL {
		return entry.name
	}
	return ""
}

func fetchContainerNames() map[string]string {
	socketPath := linuxSocketPath()
	if socketPath == "" {
		return nil
	}

	client := &http.Client{
		Transport: &http.Transport{
			DialContext: func(ctx context.Context, _, _ string) (net.Conn, error) {
				var dialer net.Dialer
				return dialer.DialContext(ctx, "unix", socketPath)
			},
		},
		Timeout: 3 * time.Second,
	}

	resp, err := client.Get("http://localhost/containers/json?all=true")
	if err != nil {
		return nil
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil
	}

	var entries []struct {
		ID    string   `json:"Id"`
		Names []string `json:"Names"`
	}
	if err := json.Unmarshal(body, &entries); err != nil {
		return nil
	}

	out := make(map[string]string, len(entries))
	for _, entry := range entries {
		id := strings.TrimPrefix(entry.ID, "/")
		if id == "" {
			continue
		}
		name := ""
		if len(entry.Names) > 0 {
			name = strings.TrimPrefix(entry.Names[0], "/")
		}
		if name == "" && len(id) > 12 {
			name = id[:12]
		}
		out[id] = name
	}
	return out
}

func linuxSocketPath() string {
	candidates := []string{
		"/var/run/docker.sock",
		filepath.Join(os.Getenv("MONITOR_HOST_ROOT"), "var/run/docker.sock"),
	}
	for _, path := range candidates {
		if path == "" {
			continue
		}
		if info, err := os.Stat(path); err == nil && info.Mode()&os.ModeSocket != 0 {
			return path
		}
	}
	return ""
}

var dockerSocketWarn sync.Once

func warnDockerSocketOnce() {
	dockerSocketWarn.Do(func() {
		slog.Debug("docker socket unavailable for container name resolution")
	})
}
