package ingest

import (
	"net/http"
	"strings"
	"sync/atomic"
)

const (
	featuresHeader     = "X-Monitor-Features"
	featureGzipIngest  = "gzip-ingest"
	featureBatchIngest = "batch-ingest"
)

// serverFeatures holds the capabilities the API advertised on its last successful response.
// It stays empty until the API is known to support them, so an older self-hosted API
// keeps receiving exactly what it always did.
var serverFeatures atomic.Pointer[map[string]struct{}]

func recordServerFeatures(header http.Header) {
	features := make(map[string]struct{})
	for _, feature := range strings.Split(header.Get(featuresHeader), ",") {
		if feature = strings.ToLower(strings.TrimSpace(feature)); feature != "" {
			features[feature] = struct{}{}
		}
	}
	serverFeatures.Store(&features)
}

// serverFeaturesKnown reports whether the API has answered at least once.
func serverFeaturesKnown() bool {
	return serverFeatures.Load() != nil
}

func serverSupports(feature string) bool {
	features := serverFeatures.Load()
	if features == nil {
		return false
	}
	_, ok := (*features)[feature]
	return ok
}
