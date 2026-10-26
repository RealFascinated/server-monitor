# Monitor Agent

Collects CPU, memory, disk, network, GPU, and Docker stats from a host and pushes them to your Monitor API.

## Get an ingest token

In the web dashboard, create a server and copy its ingest token. You need this before installing.

If you self-host, set the API URL to your instance (see [Configuration](#configuration) below).

## Install

**Linux**

```bash
curl -fsSL https://raw.githubusercontent.com/RealFascinated/server-monitor/master/agent/install.sh | sudo bash -s -- install YOUR_INGEST_TOKEN
```

**Windows** (Administrator PowerShell)

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/server-monitor/master/agent/install.ps1 -UseBasicParsing).Content)) install YOUR_INGEST_TOKEN
```

Uninstall:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/server-monitor/master/agent/install.ps1 -UseBasicParsing).Content)) uninstall
```

Windows installs as a service. It must run as Administrator for hardware sensors (including GPU).

## Configuration

Optional `config.yml` (see `config-example.yml`). Environment variables override the file: `MONITOR_` + uppercase key (e.g. `ingest_token` → `MONITOR_INGEST_TOKEN`).

| Setting | Env var | Default |
| --- | --- | --- |
| Ingest token | `MONITOR_INGEST_TOKEN` | *(required)* |
| API URL | `MONITOR_API_ENDPOINT` | `https://monitor.fascinated.cc/api/v1/servers/ingest` |
| Push interval | `MONITOR_PUSH_SCHEDULE` | `*/5 * * * * *` (every 5s) |
| Docker stats | `MONITOR_ENABLE_DOCKER` | `true` |
| GPU stats | `MONITOR_ENABLE_GPU` | `true` |
| TCP connection states | `MONITOR_ENABLE_TCP` | `true` |
| Temperature sensors | `MONITOR_ENABLE_THERMAL` | `true` |
| Per-core CPU usage | `MONITOR_ENABLE_CPU_CORES` | `true` |
| `docker stats` CLI fallback (slow; only used when cgroup stats find no containers) | `MONITOR_ENABLE_DOCKER_CLI_FALLBACK` | `true` |
| Battery | `MONITOR_ENABLE_BATTERY` | `true` |
| Disk health (SMART, NVMe wear and temperature) via `smartctl`; Linux, needs root | `MONITOR_ENABLE_SMART` | `true` |
| Clock sync status (reads the kernel, chrony or Windows Time; no network requests) | `MONITOR_ENABLE_TIMESYNC` | `true` |
| Buffer samples while the API is unreachable and replay them afterwards | `MONITOR_ENABLE_PUSH_BUFFER` | `true` |
| Oldest sample to keep in the buffer (the API ignores anything older than 6h) | `MONITOR_BUFFER_MAX_AGE` | `6h` |
| Memory the buffer may use (samples are stored compressed) | `MONITOR_BUFFER_MAX_BYTES` | `16MiB` |
| Cap on buffered samples (optional; age and size already bound it) | `MONITOR_BUFFER_MAX_SAMPLES` | *(none)* |
| Go pprof listen address (debugging; bind to loopback only) | `MONITOR_PPROF_ADDR` | *(off)* |

### Disk health, clock sync and container I/O

- **SMART:** needs `smartctl` (`smartmontools`) installed on the host and the agent running as root. The Docker images include it; the default compose agent is `privileged` with `/dev` mounted, which is enough. Disks are queried every 10 minutes with `-n standby`, so sleeping drives are not woken. Serial numbers are never read or sent.
- **Clock sync:** whether the kernel considers the clock synchronised and its own error estimate on Linux, plus the measured offset when `chronyc` is available (an agent in a container only sees it if `chronyc` is in the image); `w32tm` on Windows. No network requests are made.
- **Container network and disk I/O:** disk throughput is read from each container's cgroup. Network traffic is read through the container's first process, so a containerised agent needs `pid: host`; containers on host networking are skipped because their counters are the host's.

### Stall pressure

CPU, memory and I/O stall pressure (Linux Pressure Stall Information) is read from `/proc/pressure`, so it needs a kernel with PSI available: `CONFIG_PSI=y`, and on distributions that compile it in but leave it off, `psi=1` on the kernel command line. Nothing is reported when the files are missing. Inside an LXC container the family is skipped, because those files describe the host rather than the container.

### Compression and buffering

Both depend on the API advertising support (`X-Monitor-Features`), so a self-hosted API that predates them keeps receiving exactly what it always did.

- **Compression:** request bodies are gzip-compressed once the API has said it accepts them. If a proxy in front of the API rejects compressed bodies, the agent falls back to plain JSON until it restarts.
- **Buffering:** if a push fails because the API is unreachable (network error, timeout, 5xx, 429), the sample is kept in memory and retried with a growing delay (2s up to 1 minute). When the API answers again, the backlog is sent oldest-first through `/v1/servers/ingest/batch` and the newest sample goes through the normal endpoint. This fills graph gaps; it does not mark the server online while it was unreachable. The buffer is memory-only, so an agent restart discards it. When it is full, the oldest samples are dropped first. Requests the API refuses outright (for example an invalid token) are dropped, not retried.

Self-hosted example:

```bash
export MONITOR_API_ENDPOINT=http://your-server:8080/v1/servers/ingest
```

Or pass `--api-endpoint` when using `install.sh` / `-ApiEndpoint` with `install.ps1`. The dashboard agent tab includes the correct URL automatically.

## Docker

Image: `ghcr.io/realfascinated/monitor-agent`

- Default image: AMD/Intel GPU via sysfs
- `:nvidia` tag: NVIDIA hosts with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)

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
