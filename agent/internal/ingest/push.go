package ingest

import (
	"encoding/json"
	"fmt"
)

// Push sends one sample and returns the size of the JSON payload.
func Push(config *Config, data Data, agentVersion string) (int, error) {
	body, err := json.Marshal(data)
	if err != nil {
		return 0, fmt.Errorf("marshal ingest data: %w", err)
	}
	return len(body), postIngest("push ingest data", config.ApiEndpoint, config.IngestToken, agentVersion, body)
}
