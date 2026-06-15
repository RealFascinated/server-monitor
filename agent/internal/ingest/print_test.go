package ingest

import (
	"bytes"
	"encoding/json"
	"os"
	"testing"
)

func TestPrint(t *testing.T) {
	var buf bytes.Buffer
	data := Data{
		AgentVersion: "2.0.0",
		ServerDetails: ServerDetails{
			Ip: "127.0.0.1",
		},
	}
	if err := printTo(&buf, data); err != nil {
		t.Fatalf("printTo: %v", err)
	}

	var decoded Data
	if err := json.Unmarshal(buf.Bytes(), &decoded); err != nil {
		t.Fatalf("unmarshal output: %v", err)
	}
	if decoded.AgentVersion != "2.0.0" {
		t.Fatalf("agentVersion: got %q", decoded.AgentVersion)
	}
	if decoded.ServerDetails.Ip != "127.0.0.1" {
		t.Fatalf("ip: got %q", decoded.ServerDetails.Ip)
	}
	if !bytes.Contains(buf.Bytes(), []byte("\n  ")) {
		t.Fatalf("expected indented JSON, got %q", buf.Bytes())
	}
}

func TestLoadConfigForPrintNoToken(t *testing.T) {
	dir := t.TempDir()
	t.Chdir(dir)

	unsetConfigEnv(t)
	os.Unsetenv(configFileEnvVar)

	cfg, err := LoadConfigForPrint()
	if err != nil {
		t.Fatalf("LoadConfigForPrint: %v", err)
	}
	if cfg.IngestToken != "" {
		t.Fatalf("expected empty token, got %q", cfg.IngestToken)
	}
}
