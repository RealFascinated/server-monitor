//go:build linux

package linux

import (
	"os"
	"testing"
)

func TestHostPath(t *testing.T) {
	t.Setenv(hostRootEnvVar, "/host")

	if got := HostPath("/mnt/user"); got != "/host/mnt/user" {
		t.Fatalf("HostPath() = %q, want /host/mnt/user", got)
	}
	if got := HostPath(""); got != "" {
		t.Fatalf("HostPath(\"\") = %q, want empty", got)
	}
}

func TestMountInfoPath(t *testing.T) {
	t.Setenv(hostRootEnvVar, "/host")
	if got := MountInfoPath(); got != "/host/proc/1/mountinfo" {
		t.Fatalf("MountInfoPath() = %q, want /host/proc/1/mountinfo", got)
	}

	t.Setenv(hostRootEnvVar, "")
	if got := MountInfoPath(); got != "/proc/self/mountinfo" {
		t.Fatalf("MountInfoPath() = %q, want /proc/self/mountinfo", got)
	}
}

func TestHostRootUnset(t *testing.T) {
	os.Unsetenv(hostRootEnvVar)
	if got := HostRoot(); got != "" {
		t.Fatalf("HostRoot() = %q, want empty", got)
	}
}
