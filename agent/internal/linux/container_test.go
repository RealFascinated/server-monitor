//go:build linux

package linux

import "testing"

func TestEnvironHasLXC(t *testing.T) {
	cases := map[string]bool{
		"PATH=/bin\x00container=lxc\x00HOME=/root\x00": true,
		"container=lxc-libvirt\x00":                    true,
		"container=docker\x00":                         false,
		"PATH=/bin\x00HOME=/root\x00":                  false,
		"":                                             false,
	}
	for environ, want := range cases {
		if got := environHasLXC([]byte(environ)); got != want {
			t.Errorf("environHasLXC(%q) = %v, want %v", environ, got, want)
		}
	}
}

func TestIsLXCVirt(t *testing.T) {
	cases := map[string]bool{"lxc\n": true, "lxc-libvirt": true, "docker\n": false, "none\n": false, "": false}
	for name, want := range cases {
		if got := isLXCVirt(name); got != want {
			t.Errorf("isLXCVirt(%q) = %v, want %v", name, got, want)
		}
	}
}
