package collector

import (
	"sync"
	"testing"
	"time"
)

func TestFastAndSlowLocksDoNotDeadlock(t *testing.T) {
	s := NewSampler(Options{EnableDocker: false, EnableGPU: false})

	var wg sync.WaitGroup
	wg.Add(2)
	go func() {
		defer wg.Done()
		if err := s.Tick(); err != nil {
			t.Errorf("tick: %v", err)
		}
	}()
	go func() {
		defer wg.Done()
		if err := s.RefreshSlow(); err != nil {
			t.Errorf("refresh slow: %v", err)
		}
	}()

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()

	select {
	case <-done:
	case <-time.After(30 * time.Second):
		t.Fatal("deadlock between tick and refresh slow")
	}
}
