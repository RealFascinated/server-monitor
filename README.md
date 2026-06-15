# Monitor

Server monitoring: API, web dashboard, and host agent. Collect metrics from your machines and view them in one place.

![Servers](https://cdn.fascinated.cc/rSmesawI.png)

## Install with Docker

You need [Docker Engine](https://docs.docker.com/engine/install/) and the [Compose plugin](https://docs.docker.com/compose/install/linux/).

**1. Download the stack files**

```bash
mkdir -p monitor/docker && cd monitor

curl -fsSLO https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/docker-compose.yml
curl -fsSLO https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/.env.example
curl -fsSLO https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/docker/application.yml
```

**2. Configure environment**

```bash
cp .env.example .env
```

Edit `.env`. Change `POSTGRES_PASSWORD` and `VM_DELETE_KEY` before exposing this on a network. Default ports: `3000` (web), `8080` (API).

**3. Start**

```bash
docker compose pull
docker compose up -d
```

**4. Set up**

1. Open [http://localhost:3000](http://localhost:3000)
2. Register — the first account is admin
3. Create a server in the UI and copy the **ingest token**
4. Install the [agent](agent/README.md) on each host you want to monitor

Stop the stack: `docker compose down`. Add `-v` to delete database and metrics data.

## Monitor a host

See [agent/README.md](agent/README.md) for Linux, Windows, Docker, and Unraid install.

Quick Linux install (replace the token):

```bash
curl -fsSL https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.sh | sudo bash -s -- install YOUR_INGEST_TOKEN
```

Point `MONITOR_API_ENDPOINT` (or `api_endpoint` in config) at your API if you are not using the hosted instance.

## Development

Clone this repo if you want to build or hack on Monitor.

| Part | Location | Run locally |
| --- | --- | --- |
| API | `/` | `./gradlew bootRun` (needs Postgres + VictoriaMetrics) |
| Web | `www/` | `bun install && bun dev` (needs API on port 8080) |
| Agent | `agent/` | `go build -o monitor-agent ./cmd/main.go` |

Published images: `ghcr.io/realfascinated/monitor-api`, `monitor-www`, `monitor-agent`.
