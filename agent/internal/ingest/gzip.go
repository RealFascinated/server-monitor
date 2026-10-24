package ingest

import (
	"bytes"
	"compress/gzip"
	"log/slog"
	"sync/atomic"
)

// gzipRejected is set once a compressed push was refused while the same payload sent plain
// was accepted (for example a proxy that strips Content-Encoding). Compression then stays
// off until the agent restarts.
var gzipRejected atomic.Bool

func gzipBytes(body []byte) ([]byte, error) {
	var buf bytes.Buffer
	writer, err := gzip.NewWriterLevel(&buf, gzip.BestSpeed)
	if err != nil {
		return nil, err
	}
	if _, err := writer.Write(body); err != nil {
		return nil, err
	}
	if err := writer.Close(); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func gzipEnabled() bool {
	return serverSupports(featureGzipIngest) && !gzipRejected.Load()
}

func isEncodingRejection(err error) bool {
	statusErr, ok := err.(*httpStatusError)
	return ok && (statusErr.status == 400 || statusErr.status == 415)
}

// postIngest sends body, compressing it when the API has advertised support. If the API
// refuses the compressed body, the plain body is sent once; only when that succeeds is
// compression turned off, so a genuinely invalid payload does not disable it.
func postIngest(op, url, token, agentVersion string, body []byte) error {
	return postIngestAttempts(maxRetries, op, url, token, agentVersion, body)
}

func postIngestAttempts(attempts int, op, url, token, agentVersion string, body []byte) error {
	if gzipEnabled() {
		if compressed, err := gzipBytes(body); err == nil {
			err = postAttempts(attempts, op, url, token, agentVersion, compressed, "gzip")
			if !isEncodingRejection(err) {
				return err
			}
			if plainErr := postAttempts(attempts, op, url, token, agentVersion, body, ""); plainErr != nil {
				return plainErr
			}
			gzipRejected.Store(true)
			slog.Warn("api rejected gzip request body; disabling compression until restart", "err", err)
			return nil
		}
	}
	return postAttempts(attempts, op, url, token, agentVersion, body, "")
}
