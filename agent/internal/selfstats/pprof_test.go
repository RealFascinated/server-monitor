package selfstats

import "testing"

func TestIsLoopbackHost(t *testing.T) {
	for host, want := range map[string]bool{
		"127.0.0.1": true,
		"::1":       true,
		"localhost": true,
		"0.0.0.0":   false,
		"":          false,
		"10.0.0.5":  false,
	} {
		if got := isLoopbackHost(host); got != want {
			t.Fatalf("isLoopbackHost(%q) = %v, want %v", host, got, want)
		}
	}
}
