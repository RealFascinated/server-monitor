package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"runtime"
	"syscall"

	"fascinated.cc/monitor/agent/internal/agent"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/update"
)

var agentVersion = agent.DefaultVersion

func main() {
	agent.InitLogger()

	switch runtime.GOOS {
	case "linux", "windows":
	default:
		slog.Error("unsupported operating system", "os", runtime.GOOS)
		os.Exit(1)
	}

	if len(os.Args) > 1 && os.Args[1] == "update" {
		updated, err := update.Run(agentVersion)
		if err != nil {
			slog.Error("update failed", "err", err)
			os.Exit(1)
		}
		if !updated {
			os.Exit(0)
		}
		os.Exit(0)
	}

	if len(os.Args) > 1 && os.Args[1] == "print" {
		config, err := ingest.LoadConfigForPrint()
		if err != nil {
			slog.Error("load config", "err", err)
			os.Exit(1)
		}
		agent.New(config, agentVersion).PrintOnce()
		return
	}

	config, err := ingest.LoadConfig()
	if err != nil {
		slog.Error("load config", "err", err)
		os.Exit(1)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	agent.New(config, agentVersion).Run(ctx)
}
