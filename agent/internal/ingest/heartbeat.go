package ingest

func Heartbeat(config *Config, agentVersion string) error {
	return post("heartbeat", config.HeartbeatURL(), config.IngestToken, agentVersion, nil)
}
