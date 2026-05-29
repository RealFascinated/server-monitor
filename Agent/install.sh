#!/usr/bin/env bash
set -euo pipefail

GITHUB_REPO="RealFascinated/Monitor-API"
DEFAULT_VERSION="2.0.0"
DEFAULT_API_ENDPOINT="https://monitor.fascinated.cc/api/v1/servers/ingest"
INSTALL_BIN="/usr/local/bin/monitor-agent"
CONFIG_DIR="/etc/monitor-agent"
CONFIG_FILE="${CONFIG_DIR}/config.yml"
SERVICE_NAME="monitor-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

usage() {
  cat <<EOF
Install the Monitor agent on Linux.

Usage:
  sudo $0 <ingest_token> [options]

Options:
  --version VERSION       Agent release version (default: latest, fallback ${DEFAULT_VERSION})
  --api-endpoint URL      Ingest API endpoint (default: ${DEFAULT_API_ENDPOINT})
  --auto-update VALUE     Daily self-updates (default: true)
  -h, --help              Show this help message

Example:
  curl -fsSL https://github.com/${GITHUB_REPO}/releases/download/agent/v${DEFAULT_VERSION}/install.sh | sudo bash -s -- YOUR_INGEST_TOKEN
EOF
}

log() {
  printf '==> %s\n' "$*"
}

die() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

require_root() {
  if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    die "run as root (e.g. sudo $0 <ingest_token>)"
  fi
}

require_linux() {
  case "$(uname -s)" in
    Linux) ;;
    *)
      die "this installer only supports Linux"
      ;;
  esac
}

detect_arch() {
  case "$(uname -m)" in
    x86_64|amd64) echo "amd64" ;;
    aarch64|arm64) echo "arm64" ;;
    *)
      die "unsupported architecture: $(uname -m)"
      ;;
  esac
}

escape_yaml_string() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

fetch_latest_version() {
  local releases tag
  releases="$(curl -fsSL "https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=100")"
  tag="$(printf '%s\n' "$releases" | grep -oE '"tag_name"[[:space:]]*:[[:space:]]*"agent/v[^"]+"' | head -1 | sed -E 's/.*"agent\/v([^"]+)".*/\1/')"
  if [[ -z "$tag" ]]; then
    echo "$DEFAULT_VERSION"
    return
  fi
  echo "$tag"
}

download_release() {
  local version="$1"
  local arch="$2"
  local tmpdir="$3"
  local tag="agent/v${version}"
  local asset="monitor-agent-linux-${arch}"
  local base_url="https://github.com/${GITHUB_REPO}/releases/download/${tag}"
  local checksums_url="${base_url}/checksums.txt"
  local asset_url="${base_url}/${asset}"

  log "Downloading ${asset} (${tag})"
  curl -fsSL "$asset_url" -o "${tmpdir}/${asset}"

  if curl -fsSL "$checksums_url" -o "${tmpdir}/checksums.txt"; then
    log "Verifying checksum"
    (
      cd "$tmpdir"
      grep " ${asset}\$" checksums.txt | sha256sum -c -
    )
  else
    log "Checksum file unavailable; skipping verification"
  fi

  printf '%s' "${tmpdir}/${asset}"
}

write_config() {
  local ingest_token="$1"
  local api_endpoint="$2"
  local token_escaped endpoint_escaped

  token_escaped="$(escape_yaml_string "$ingest_token")"
  endpoint_escaped="$(escape_yaml_string "$api_endpoint")"

  install -d -m 0755 "$CONFIG_DIR"
  cat >"$CONFIG_FILE" <<EOF
ingest_token: "${token_escaped}"
api_endpoint: "${endpoint_escaped}"
push_schedule: "*/15 * * * * *"
enable_docker: true
EOF
  chmod 0600 "$CONFIG_FILE"
}

write_service() {
  cat >"$SERVICE_FILE" <<EOF
[Unit]
Description=Monitor Agent
After=network-online.target docker.service
Wants=network-online.target

[Service]
Type=simple
ExecStart=${INSTALL_BIN}
Environment=MONITOR_CONFIG_FILE=${CONFIG_FILE}
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
}

write_update_timer() {
  cat >"/etc/systemd/system/${SERVICE_NAME}-update.service" <<EOF
[Unit]
Description=Monitor Agent Self Update
After=network-online.target

[Service]
Type=oneshot
ExecStart=${INSTALL_BIN} update

[Install]
WantedBy=multi-user.target
EOF

  cat >"/etc/systemd/system/${SERVICE_NAME}-update.timer" <<EOF
[Unit]
Description=Daily Monitor Agent update check

[Timer]
OnCalendar=daily
Persistent=true
RandomizedDelaySec=4h

[Install]
WantedBy=timers.target
EOF

  systemctl daemon-reload
  systemctl enable --now "${SERVICE_NAME}-update.timer"
}

INGEST_TOKEN=""
VERSION=""
API_ENDPOINT="$DEFAULT_API_ENDPOINT"
AUTO_UPDATE="true"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      [[ $# -ge 2 ]] || die "--version requires a value"
      VERSION="$2"
      shift 2
      ;;
    --api-endpoint)
      [[ $# -ge 2 ]] || die "--api-endpoint requires a value"
      API_ENDPOINT="$2"
      shift 2
      ;;
    --auto-update)
      if [[ "$1" == *=* ]]; then
        AUTO_UPDATE="${1#*=}"
        shift
      elif [[ $# -ge 2 ]]; then
        AUTO_UPDATE="$2"
        shift 2
      else
        die "--auto-update requires true or false"
      fi
      if [[ "$AUTO_UPDATE" != "true" && "$AUTO_UPDATE" != "false" ]]; then
        die "--auto-update must be true or false"
      fi
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      die "unknown option: $1"
      ;;
    *)
      if [[ -n "$INGEST_TOKEN" ]]; then
        die "unexpected argument: $1"
      fi
      INGEST_TOKEN="$1"
      shift
      ;;
  esac
done

[[ -n "$INGEST_TOKEN" ]] || {
  usage
  die "ingest token is required"
}

require_root
require_linux

ARCH="$(detect_arch)"
TMPDIR="$(mktemp -d)"
trap 'rm -rf "$TMPDIR"' EXIT

if [[ -z "$VERSION" ]]; then
  VERSION="$(fetch_latest_version)"
fi

DOWNLOADED="$(download_release "$VERSION" "$ARCH" "$TMPDIR")"

log "Installing binary to ${INSTALL_BIN}"
install -d -m 0755 "$(dirname "$INSTALL_BIN")"
install -m 0755 "$DOWNLOADED" "$INSTALL_BIN"

log "Writing config to ${CONFIG_FILE}"
write_config "$INGEST_TOKEN" "$API_ENDPOINT"

log "Installing systemd service"
write_service
systemctl daemon-reload
systemctl enable --now "$SERVICE_NAME"

if [[ "$AUTO_UPDATE" == "true" ]]; then
  log "Enabling daily self-updates"
  write_update_timer
else
  log "Skipping daily self-updates"
fi

log "Monitor agent installed and started"
systemctl --no-pager status "$SERVICE_NAME"
