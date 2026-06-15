package zfs

import "testing"

func TestPoolName(t *testing.T) {
	if got := PoolName("tank/home"); got != "tank" {
		t.Fatalf("expected tank, got %q", got)
	}
	if got := PoolName("tank"); got != "tank" {
		t.Fatalf("expected tank, got %q", got)
	}
}

func TestMountGetsPoolIO(t *testing.T) {
	if !MountGetsPoolIO("tank", "/") {
		t.Fatal("root should receive pool io")
	}
	if MountGetsPoolIO("tank/home", "/home") {
		t.Fatal("dataset mount should not receive pool io")
	}
}
