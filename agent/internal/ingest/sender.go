package ingest

import (
	"bytes"
	"compress/gzip"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"math/rand/v2"
	"strconv"
	"time"
)

const (
	// liveMaxAge is how old the newest sample may be and still go through the normal ingest
	// endpoint, which is what keeps the server marked online and its inventory current.
	liveMaxAge      = 30 * time.Second
	maxBatchSamples = 100
	maxBatchBytes   = 4 << 20
	minBackoff      = 2 * time.Second
	maxBackoff      = time.Minute
)

// Sender delivers samples to the API from its own goroutine so a slow or failing API never
// stalls collection. While the API is unreachable, samples wait in a bounded queue and are
// replayed oldest-first through the batch endpoint once it answers again.
type Sender struct {
	version string
	config  func() *Config

	// Observe is called after every delivery attempt with its duration, payload size and result.
	Observe func(duration time.Duration, payloadBytes int, err error)

	queue *sampleQueue
	wake  chan struct{}
	now   func() time.Time
	wait  func(ctx context.Context, d time.Duration) bool

	backoff       time.Duration
	warnedNoBatch bool
}

// errCorruptSample marks a queued sample that cannot be decoded; retrying cannot fix it.
var errCorruptSample = errors.New("corrupt queued sample")

func NewSender(version string, config func() *Config) *Sender {
	s := &Sender{
		version: version,
		config:  config,
		now:     time.Now,
		wake:    make(chan struct{}, 1),
		wait:    sleepContext,
	}
	s.queue = newSampleQueue(s.now)
	return s
}

func sleepContext(ctx context.Context, d time.Duration) bool {
	timer := time.NewTimer(d)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return false
	case <-timer.C:
		return true
	}
}

// Submit queues a sample for delivery and returns immediately.
func (s *Sender) Submit(data Data) error {
	body, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("marshal ingest data: %w", err)
	}
	s.queue.setLimits(s.limits(s.config()))
	if err := s.queue.add(body); err != nil {
		return fmt.Errorf("queue ingest data: %w", err)
	}
	select {
	case s.wake <- struct{}{}:
	default:
	}
	return nil
}

// Stats reports the state of the push buffer.
func (s *Sender) Stats() QueueStats {
	return s.queue.stats()
}

// Run delivers queued samples until ctx is cancelled.
func (s *Sender) Run(ctx context.Context) {
	for {
		s.drain(ctx)
		select {
		case <-ctx.Done():
			return
		case <-s.wake:
		}
	}
}

// buffering is false when samples should be sent once and dropped on failure: the operator
// turned the buffer off, or the API has told us it cannot accept backfilled samples.
func (s *Sender) buffering(config *Config) bool {
	if !config.PushBuffer.Enabled {
		return false
	}
	if serverFeaturesKnown() && !serverSupports(featureBatchIngest) {
		if !s.warnedNoBatch {
			s.warnedNoBatch = true
			slog.Info("api does not support batch ingest; failed pushes will be dropped, not buffered")
		}
		return false
	}
	return true
}

func (s *Sender) limits(config *Config) Limits {
	if !s.buffering(config) {
		return Limits{MaxSamples: 1}
	}
	return config.PushBuffer.Limits()
}

func (s *Sender) drain(ctx context.Context) {
	for ctx.Err() == nil {
		config := s.config()
		buffering := s.buffering(config)
		if !buffering {
			s.queue.keepNewest(1)
		}

		items, total := s.queue.peek(maxBatchSamples)
		if len(items) == 0 {
			s.backoff = 0
			return
		}

		newest := items[len(items)-1]
		live := !buffering || (len(items) == total && s.now().Sub(newest.queuedAt) <= liveMaxAge)
		backlog := items
		if live {
			backlog = items[:len(items)-1]
		}

		var (
			sent []queuedSample
			err  error
		)
		if len(backlog) > 0 {
			var count int
			count, err = s.sendBatch(config, backlog)
			sent = backlog[:count]
		} else {
			attempts := 1
			if !buffering {
				attempts = maxRetries
			}
			sent = items[len(items)-1:]
			err = s.sendLive(config, newest, attempts)
		}

		if err == nil {
			s.queue.removeThrough(sent[len(sent)-1].seq)
			s.backoff = 0
			if len(backlog) > 0 {
				slog.Info("buffered metrics delivered", "samples", len(sent), "remaining", total-len(sent))
			} else {
				slog.Info("metrics pushed")
			}
			continue
		}

		if buffering && shouldRetry(err) && !errors.Is(err, errCorruptSample) {
			s.backoff = nextBackoff(s.backoff)
			slog.Warn("push failed, keeping samples for retry", "err", err, "buffered", total, "retry_in", s.backoff.Round(time.Second))
			if !s.wait(ctx, jitter(s.backoff)) {
				return
			}
			continue
		}

		// Either buffering is off (the single attempt already retried), or the API refused the
		// data outright; resending the same bytes cannot succeed.
		slog.Error("push metrics", "err", err, "dropped", len(sent))
		s.queue.discardThrough(sent[len(sent)-1].seq)
		s.backoff = 0
	}
}

func (s *Sender) sendLive(config *Config, sample queuedSample, attempts int) error {
	body, err := gunzip(sample.gz)
	if err != nil {
		return fmt.Errorf("%w: %v", errCorruptSample, err)
	}
	return s.attempt(len(body), func() error {
		return postIngestAttempts(attempts, "push ingest data", config.ApiEndpoint, config.IngestToken, s.version, body)
	})
}

// sendBatch posts as many of samples as fit in one request and returns how many it included.
func (s *Sender) sendBatch(config *Config, samples []queuedSample) (int, error) {
	var body bytes.Buffer
	body.WriteString(`{"sentAt":` + strconv.FormatInt(s.now().Unix(), 10) + `,"samples":[`)
	count := 0
	for _, sample := range samples {
		raw, err := gunzip(sample.gz)
		if err != nil {
			return 1, fmt.Errorf("%w: %v", errCorruptSample, err)
		}
		if count > 0 && body.Len()+len(raw) > maxBatchBytes {
			break
		}
		if count > 0 {
			body.WriteByte(',')
		}
		body.Write(raw)
		count++
	}
	body.WriteString(`]}`)

	err := s.attempt(body.Len(), func() error {
		return postIngestAttempts(1, "push ingest batch", config.BatchURL(), config.IngestToken, s.version, body.Bytes())
	})
	return count, err
}

func (s *Sender) attempt(payloadBytes int, send func() error) error {
	started := time.Now()
	err := send()
	if s.Observe != nil {
		s.Observe(time.Since(started), payloadBytes, err)
	}
	return err
}

func gunzip(data []byte) ([]byte, error) {
	reader, err := gzip.NewReader(bytes.NewReader(data))
	if err != nil {
		return nil, err
	}
	defer reader.Close()
	return io.ReadAll(reader)
}

func nextBackoff(current time.Duration) time.Duration {
	if current < minBackoff {
		return minBackoff
	}
	return min(current*2, maxBackoff)
}

func jitter(d time.Duration) time.Duration {
	return time.Duration(float64(d) * (0.8 + 0.4*rand.Float64()))
}
