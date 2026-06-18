# Monitor

Server monitoring: API, web dashboard, and host agent. Collect metrics from your machines and view them in one place.

![Servers](https://cdn.fascinated.cc/rSmesawI.png)

## Install with Docker

You need [Docker Engine](https://docs.docker.com/engine/install/) and the [Compose plugin](https://docs.docker.com/compose/install/linux/).

**1. Clone and configure**

```bash
git clone https://github.com/RealFascinated/Monitor-API.git monitor
cd monitor
cp .env.example .env
```

Edit `.env` and set `MONITOR_ENCRYPTION_SECRET` (`openssl rand -hex 32`), plus `POSTGRES_PASSWORD` and `VM_DELETE_KEY` before exposing on a network.

**2. Start**

```bash
docker compose pull
docker compose up -d
```

**3. Set up**

1. Open [http://localhost:3000](http://localhost:3000) (web UI)
2. Register — the first account is admin
3. Create a server in the UI and copy the **ingest token**
4. Install the [agent](agent/README.md) on each host you want to monitor

Stop the stack: `docker compose down`. Add `-v` to delete database and metrics data.

### Self-hosting notes

Web UI: port `3000`. API: port `8080`. Set `MONITOR_PUBLIC_API_URL` to the URL browsers and agents use to reach the API.

**After first login**, consider disabling open registration under **Admin → Settings** if this is a private instance.

#### Optional configuration

Set these in `.env` or `docker/application.yml` as needed:

| Feature | Config |
| --- | --- |
| Email (password reset, server invites) | `monitor.mail.enabled: true` plus SMTP host/port/credentials in `docker/application.yml` |
| Session location labels | `MAXMIND_LICENSE` — free [GeoLite2](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data) license key |
| Agent updates from your fork | `monitor.agent.github-owner` / `github-repo` in `docker/application.yml` |

When mail is disabled, password-reset and invite links are written to the **API container logs** instead of being emailed.

#### Production checklist

- Put TLS in front of both the web UI and API (or terminate at your reverse proxy)
- Set `MONITOR_WEBSITE_URL` and `MONITOR_PUBLIC_API_URL` to your public HTTPS URLs
- Do not expose VictoriaMetrics (`8428`) publicly — remove that port mapping or firewall it
- Back up the `postgres_data` and `victoriametrics_data` Docker volumes regularly
- Use the install commands from the dashboard (they include your ingest API URL) or pass `--api-endpoint` / `MONITOR_API_ENDPOINT` manually

## Monitor a host

See [agent/README.md](agent/README.md) for Linux, Windows, Docker, and Unraid install.

The dashboard **Agent** tab copies install commands with the correct API endpoint for your instance. For a manual Linux install against a self-hosted stack:

```bash
curl -fsSL https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.sh | \
  sudo bash -s -- install YOUR_INGEST_TOKEN --api-endpoint http://your-host:8080/v1/servers/ingest
```

## Development

Clone this repo if you want to build or hack on Monitor.

| Part | Location | Run locally |
| --- | --- | --- |
| API | `/` | `./gradlew bootRun` (needs Postgres + VictoriaMetrics) |
| Web | `www/` | `bun install && bun dev` (needs API on port 8080) |
| Agent | `agent/` | `go build -o monitor-agent ./cmd/main.go` |

Published images: `ghcr.io/realfascinated/monitor-api`, `monitor-www`, `monitor-agent`.
