# Monitor Agent

Collects CPU, memory, disk, network, GPU, and Docker stats from a host and pushes them to your Monitor API.

## Get an ingest token

In the web dashboard, create a server and copy its ingest token. You need this before installing.

If you self-host, set the API URL to your instance (see [Configuration](#configuration) below).

## Install

**Linux**

```bash
curl -fsSL https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.sh | sudo bash -s -- install YOUR_INGEST_TOKEN
```

**Windows** (Administrator PowerShell)

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.ps1 -UseBasicParsing).Content)) install YOUR_INGEST_TOKEN
```

Uninstall:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.ps1 -UseBasicParsing).Content)) uninstall
```

Windows installs as a service. It must run as Administrator for hardware sensors (including GPU).

## Configuration

Optional `config.yml` (see `config-example.yml`). Environment variables override the file: `MONITOR_` + uppercase key (e.g. `ingest_token` → `MONITOR_INGEST_TOKEN`).

| Setting | Env var | Default |
| --- | --- | --- |
| Ingest token | `MONITOR_INGEST_TOKEN` | *(required)* |
| API URL | `MONITOR_API_ENDPOINT` | `https://monitor.fascinated.cc/api/v1/servers/ingest` |
| Push interval | `MONITOR_PUSH_SCHEDULE` | `*/15 * * * * *` (every 15s) |
| Docker stats | `MONITOR_ENABLE_DOCKER` | `true` |
| GPU stats | `MONITOR_ENABLE_GPU` | `true` |

Self-hosted example:

```bash
export MONITOR_API_ENDPOINT=http://your-server:8080/api/v1/servers/ingest
```

## Docker

Image: `ghcr.io/realfascinated/monitor-agent`

- Default image — AMD/Intel GPU via sysfs
- `:nvidia` tag — NVIDIA hosts with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)

The root `docker-compose.yml` has a commented **agent** service. Copy the env vars from there, or run with the agent profile after setting `MONITOR_INGEST_TOKEN` in `.env`:

```bash
docker compose --profile agent up -d
```

The container needs host mounts (`/proc`, `/sys`, `/dev`, `/`, Docker socket) and `privileged: true` for full metrics. See the compose file for the full list.

## Unraid

In **Apps**, search for `monitor-agent` (or `monitor-agent-nvidia` for NVIDIA hosts with the Unraid NVIDIA Driver plugin). Install from Community Applications, paste your ingest token, and apply.

## Debug

Print one metrics payload without pushing:

```bash
./monitor-agent print
```
