//go:build linux

package zfs

import (
	"bufio"
	"bytes"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
)

func ReadPoolIOSnapshots() map[string]PoolIO {
	snapshots := make(map[string]PoolIO)
	matches, err := filepath.Glob("/proc/spl/kstat/zpool/*/io")
	if err != nil {
		return snapshots
	}

	for _, path := range matches {
		pool := filepath.Base(filepath.Dir(path))
		io, err := parsePoolIO(path)
		if err != nil {
			continue
		}
		snapshots[pool] = io
	}
	return snapshots
}

func parsePoolIO(path string) (PoolIO, error) {
	file, err := os.Open(path)
	if err != nil {
		return PoolIO{}, err
	}
	defer file.Close()

	var io PoolIO
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 2 {
			continue
		}
		switch fields[0] {
		case "nread":
			io.Nread = parseUint64(fields[len(fields)-1])
		case "nwritten":
			io.Nwritten = parseUint64(fields[len(fields)-1])
		}
	}
	return io, scanner.Err()
}

func StartPoolIostatSample() func() map[string]PoolIORates {
	if _, err := exec.LookPath("zpool"); err != nil {
		return func() map[string]PoolIORates {
			return map[string]PoolIORates{}
		}
	}

	done := make(chan map[string]PoolIORates, 1)
	go func() {
		out, err := exec.Command("zpool", "iostat", "-yqHp", "1", "1").Output()
		if err != nil {
			done <- map[string]PoolIORates{}
			return
		}
		done <- parsePoolIostatOutput(out)
	}()

	return func() map[string]PoolIORates {
		return <-done
	}
}

func parsePoolIostatOutput(out []byte) map[string]PoolIORates {
	rates := make(map[string]PoolIORates)
	scanner := bufio.NewScanner(bytes.NewReader(out))
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, " ") || strings.HasPrefix(line, "\t") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 7 || strings.HasPrefix(fields[0], "/") {
			continue
		}
		rates[fields[0]] = PoolIORates{
			ReadIops:            int64(parseUint64(fields[3])),
			WriteIops:           int64(parseUint64(fields[4])),
			ReadBytesPerSecond:  int64(parseUint64(fields[5])),
			WriteBytesPerSecond: int64(parseUint64(fields[6])),
		}
	}
	return rates
}

func parseUint64(value string) uint64 {
	n, _ := strconv.ParseUint(value, 10, 64)
	return n
}
