package connections

import (
	"sort"

	"fascinated.cc/monitor/agent/internal/ingest"
)

// Tracker holds TCP socket counts keyed by kernel state name (e.g. ESTABLISHED).
type Tracker struct {
	States map[string]int64
}

func (t Tracker) ToIngest() []ingest.TCPConnectionMetric {
	if len(t.States) == 0 {
		return nil
	}
	states := make([]string, 0, len(t.States))
	for state := range t.States {
		states = append(states, state)
	}
	sort.Strings(states)

	out := make([]ingest.TCPConnectionMetric, len(states))
	for i, state := range states {
		out[i] = ingest.TCPConnectionMetric{
			State: state,
			Count: t.States[state],
		}
	}
	return out
}
