package ingest

import (
	"encoding/json"
	"fmt"
)

func Push(config *Config, data Data, agentVersion string) error {
	body, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("marshal ingest data: %w", err)
	}
	return postWithRetry("push ingest data", config.ApiEndpoint, config.IngestToken, agentVersion, body)
}
