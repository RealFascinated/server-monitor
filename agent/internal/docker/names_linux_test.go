//go:build linux

package docker

import (
	"net"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"
)

func fakeDockerDaemon(t *testing.T, body string) {
	t.Helper()
	// Unix socket paths are limited to ~108 bytes, so avoid t.TempDir().
	dir, err := os.MkdirTemp("", "dk")
	if err != nil {
		t.Fatal(err)
	}
	sock := filepath.Join(dir, "d.sock")
	listener, err := net.Listen("unix", sock)
	if err != nil {
		t.Fatal(err)
	}
	server := &http.Server{Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(body))
	})}
	go server.Serve(listener)

	previous := socketPath
	socketPath = func() string { return sock }
	t.Cleanup(func() {
		socketPath = previous
		server.Close()
		os.RemoveAll(dir)
	})
}

func TestFetchContainerNamesKeepsGoroutineCountStable(t *testing.T) {
	fakeDockerDaemon(t, `[{"Id":"abc123","Names":["/web"]}]`)

	// Warm up so lazily started runtime goroutines are not counted.
	fetchContainerNames()
	time.Sleep(50 * time.Millisecond)
	before := runtime.NumGoroutine()

	for range 100 {
		names := fetchContainerNames()
		if names["abc123"] != "web" {
			t.Fatalf("unexpected names: %v", names)
		}
	}
	time.Sleep(100 * time.Millisecond)

	if grown := runtime.NumGoroutine() - before; grown > 5 {
		t.Fatalf("goroutines grew by %d over 100 lookups; idle connections are being retained", grown)
	}
}

func TestResolveContainerNameDropsRemovedContainers(t *testing.T) {
	fakeDockerDaemon(t, `[{"Id":"live","Names":["/live-container"]}]`)

	namesCache.mu.Lock()
	namesCache.entries = map[string]containerNameEntry{
		"gone": {name: "old-container", at: time.Now().Add(-time.Hour)},
	}
	namesCache.mu.Unlock()
	t.Cleanup(func() {
		namesCache.mu.Lock()
		namesCache.entries = nil
		namesCache.mu.Unlock()
	})

	if got := resolveContainerName("live"); got != "live-container" {
		t.Fatalf("resolveContainerName = %q", got)
	}

	namesCache.mu.Lock()
	defer namesCache.mu.Unlock()
	if _, stale := namesCache.entries["gone"]; stale {
		t.Fatal("removed container was kept in the cache")
	}
	if len(namesCache.entries) != 1 {
		t.Fatalf("cache size = %d, want 1", len(namesCache.entries))
	}
}
