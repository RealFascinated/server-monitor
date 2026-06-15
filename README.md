# Monitor

Monorepo for the Monitor API, host metrics agent, and web dashboard.

| Directory | Description |
| --- | --- |
| `/` | Spring Boot API for server monitoring |
| `agent/` | Host metrics agent (Go) |
| `www/` | Web dashboard (TanStack Start) |

## API

**Docker:** `docker build -t monitor-api .`

Images are published to GitHub Container Registry on each push to `master` (as `:master`) and on `api/v*` release tags:

`ghcr.io/realfascinated/monitor-api`

## Agent

See [agent/README.md](agent/README.md).

## Web

See [www/README.md](www/README.md).
