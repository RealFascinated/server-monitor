//go:build windows && lhmbundle

package lhm

import (
	"bufio"
	"context"
	"embed"
	"fmt"
	"io"
	"io/fs"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

const (
	embedRoot     = "sensorhost/bin/Release/net48"
	helperDirName = "monitor-agent-lhm"
	commandLine   = "getServerMetrics"
)

var (
	clientOnce sync.Once
	client     *helperClient
	clientErr  error
)

type helperClient struct {
	mu      sync.Mutex
	dir     string
	cmd     *exec.Cmd
	stdin   io.WriteCloser
	stdout  *bufio.Reader
	started time.Time
}

// GetServerMetrics queries the embedded LHM helper process.
func GetServerMetrics(ctx context.Context) (ServerSnapshot, error) {
	clientOnce.Do(func() {
		client, clientErr = startHelper()
	})
	if clientErr != nil {
		return ServerSnapshot{}, clientErr
	}

	if ctx == nil {
		ctx = context.Background()
	}
	deadline, ok := ctx.Deadline()
	if !ok {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, 5*time.Second)
		defer cancel()
		deadline, _ = ctx.Deadline()
	}

	client.mu.Lock()
	defer client.mu.Unlock()

	if err := client.ensureRunning(); err != nil {
		return ServerSnapshot{}, err
	}

	if _, err := client.stdin.Write([]byte(commandLine + "\n")); err != nil {
		client.reset()
		return ServerSnapshot{}, fmt.Errorf("lhm: write command: %w", err)
	}

	lineCh := make(chan string, 1)
	errCh := make(chan error, 1)
	go func() {
		line, err := client.stdout.ReadString('\n')
		if err != nil {
			errCh <- err
			return
		}
		lineCh <- line
	}()

	select {
	case <-ctx.Done():
		client.reset()
		return ServerSnapshot{}, fmt.Errorf("lhm: %w", ctx.Err())
	case err := <-errCh:
		client.reset()
		return ServerSnapshot{}, fmt.Errorf("lhm: read response: %w", err)
	case line := <-lineCh:
		_ = deadline
		line = strings.TrimSpace(line)
		if line == "" {
			return ServerSnapshot{}, fmt.Errorf("lhm: empty response")
		}
		return ParseServerMetricsJSON([]byte(line))
	}
}

func startHelper() (*helperClient, error) {
	dir, err := extractEmbedded(embeddedSensorHost, embedRoot)
	if err != nil {
		return nil, fmt.Errorf("lhm: extract helper: %w", err)
	}

	c := &helperClient{dir: dir}
	if err := c.ensureRunning(); err != nil {
		return nil, err
	}
	return c, nil
}

func (c *helperClient) ensureRunning() error {
	if c.cmd != nil && c.cmd.Process != nil {
		return nil
	}
	return c.start()
}

func (c *helperClient) start() error {
	c.reset()

	exe := filepath.Join(c.dir, "get_sensors.exe")
	cmd := exec.Command(exe)
	cmd.Dir = c.dir

	stdin, err := cmd.StdinPipe()
	if err != nil {
		return fmt.Errorf("stdin pipe: %w", err)
	}
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		stdin.Close()
		return fmt.Errorf("stdout pipe: %w", err)
	}
	if err := cmd.Start(); err != nil {
		stdin.Close()
		return fmt.Errorf("start helper: %w", err)
	}

	c.cmd = cmd
	c.stdin = stdin
	c.stdout = bufio.NewReader(stdout)
	c.started = time.Now()
	return nil
}

func (c *helperClient) reset() {
	if c.stdin != nil {
		_ = c.stdin.Close()
		c.stdin = nil
	}
	if c.cmd != nil && c.cmd.Process != nil {
		_ = c.cmd.Process.Kill()
		_ = c.cmd.Wait()
	}
	c.cmd = nil
	c.stdout = nil
}

func extractEmbedded(bundle embed.FS, srcRoot string) (string, error) {
	dest := filepath.Join(os.TempDir(), helperDirName)
	if err := os.MkdirAll(dest, 0o755); err != nil {
		return "", err
	}

	entries, err := fs.ReadDir(bundle, srcRoot)
	if err != nil {
		return "", err
	}

	for _, entry := range entries {
		srcPath := path.Join(srcRoot, entry.Name())
		destPath := filepath.Join(dest, entry.Name())
		if entry.IsDir() {
			if err := copyEmbeddedDir(bundle, srcPath, destPath); err != nil {
				return "", err
			}
			continue
		}
		data, err := bundle.ReadFile(srcPath)
		if err != nil {
			return "", err
		}
		if err := os.WriteFile(destPath, data, 0o755); err != nil {
			return "", err
		}
	}

	return dest, nil
}

func copyEmbeddedDir(bundle embed.FS, srcPath, destPath string) error {
	if err := os.MkdirAll(destPath, 0o755); err != nil {
		return err
	}
	entries, err := fs.ReadDir(bundle, srcPath)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		srcEntry := path.Join(srcPath, entry.Name())
		destEntry := filepath.Join(destPath, entry.Name())
		if entry.IsDir() {
			if err := copyEmbeddedDir(bundle, srcEntry, destEntry); err != nil {
				return err
			}
			continue
		}
		data, err := bundle.ReadFile(srcEntry)
		if err != nil {
			return err
		}
		if err := os.WriteFile(destEntry, data, 0o755); err != nil {
			return err
		}
	}
	return nil
}
