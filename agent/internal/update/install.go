package update

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"strings"
)

func installBinary(newBinaryPath string) error {
	if runtime.GOOS == "windows" {
		if err := stopService(); err != nil {
			return fmt.Errorf("stop service: %w", err)
		}
	}

	currentPath, err := os.Executable()
	if err != nil {
		return err
	}
	currentPath, err = filepath.EvalSymlinks(currentPath)
	if err != nil {
		return err
	}

	backupPath := currentPath + ".old"
	_ = os.Remove(backupPath)

	if err := os.Rename(currentPath, backupPath); err != nil {
		return fmt.Errorf("backup current executable: %w", err)
	}

	revert := func() {
		_ = os.Remove(currentPath)
		_ = os.Rename(backupPath, currentPath)
	}

	if err := installToPath(newBinaryPath, currentPath); err != nil {
		revert()
		return fmt.Errorf("install new executable: %w", err)
	}

	if err := os.Chmod(currentPath, 0o755); err != nil {
		revert()
		return fmt.Errorf("set executable permissions: %w", err)
	}

	_ = os.Remove(backupPath)
	return nil
}

func installToPath(src, dst string) error {
	if err := os.Rename(src, dst); err == nil {
		return nil
	} else if !isCrossDeviceError(err) {
		return err
	}
	return copyFile(src, dst)
}

func isCrossDeviceError(err error) bool {
	if err == nil {
		return false
	}
	msg := err.Error()
	return strings.Contains(msg, "cross-device") || strings.Contains(msg, "EXDEV")
}

func copyFile(src, dst string) error {
	source, err := os.Open(src)
	if err != nil {
		return err
	}
	defer source.Close()

	info, err := source.Stat()
	if err != nil {
		return err
	}

	dest, err := os.OpenFile(dst, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, info.Mode().Perm())
	if err != nil {
		return err
	}
	defer dest.Close()

	if _, err := io.Copy(dest, source); err != nil {
		return err
	}

	return dest.Chmod(info.Mode().Perm())
}
