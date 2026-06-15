package ingest

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
)

func Print(data Data) error {
	return printTo(os.Stdout, data)
}

func printTo(w io.Writer, data Data) error {
	body, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return fmt.Errorf("marshal ingest data: %w", err)
	}
	body = append(body, '\n')
	if _, err := w.Write(body); err != nil {
		return fmt.Errorf("write ingest data: %w", err)
	}
	return nil
}
