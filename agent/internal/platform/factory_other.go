//go:build !linux && !windows

package platform

func New(opts Options) Backend {
	return &stubBackend{}
}

type stubBackend struct{}

func (stubBackend) Tick(bool) (TickUpdate, error) { return TickUpdate{}, nil }
func (stubBackend) RefreshSlow() (SlowUpdate, error) {
	return SlowUpdate{}, nil
}
