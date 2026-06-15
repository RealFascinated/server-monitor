#!/usr/bin/env bash
set -euo pipefail

GITHUB_REPO="RealFascinated/Monitor-API"
AGENT_PATH="agent"
DEFAULT_VERSION="2.0.1"
DEFAULT_API_ENDPOINT="https://monitor.fascinated.cc/api/v1/servers/ingest"
INSTALL_BIN="/usr/local/bin/monitor-agent"
CONFIG_DIR="/etc/monitor-agent"
CONFIG_FILE="${CONFIG_DIR}/config.yml"
SERVICE_NAME="monitor-agent"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

usage() {
  cat <<EOF
Install or remove the Monitor agent on Linux (systemd).

Usage:
  sudo $0 install <ingest_token> [options]
  sudo $0 uninstall

Commands:
  install                 Install the agent
  uninstall               Stop the agent and remove installed files

Options (install only):
  --version VERSION       Agent release version (default: latest GitHub release)
  --api-endpoint URL      Ingest API endpoint (default: ${DEFAULT_API_ENDPOINT})
  --auto-update VALUE     Daily self-updates (default: true)
  -h, --help              Show this help message

Examples:
  curl -fsSL https://github.com/${GITHUB_REPO}/releases/download/agent/v${DEFAULT_VERSION}/install.sh | sudo bash -s -- install YOUR_INGEST_TOKEN
  curl -fsSL https://github.com/${GITHUB_REPO}/releases/download/agent/v${DEFAULT_VERSION}/install.sh | sudo bash -s -- uninstall
EOF
}

log() {
  printf '==> %s\n' "$*" >&2
}

die() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

require_root() {
  if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    die "run as root (e.g. sudo $0 install <ingest_token>)"
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

require_systemd() {
  command -v systemctl >/dev/null 2>&1 || die "systemctl not found"
  [[ -d /etc/systemd/system ]] || die "/etc/systemd/system not found"
  [[ "$(ps -p 1 -o comm= 2>/dev/null | tr -d '[:space:]')" == "systemd" ]] || die "init is not systemd"
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
  [[ -n "$releases" ]] || die "could not fetch releases"

  if command -v jq >/dev/null 2>&1; then
    tag="$(
      printf '%s' "$releases" | jq -r '
        [.[] | select(.draft == false and .prerelease == false)
         | .tag_name | select(startswith("agent/v")) | sub("^agent/v"; "")]
        | .[]
      ' | sort -V | tail -n1
    )"
  else
    tag="$(
      printf '%s\n' "$releases" \
        | grep -oE '"tag_name"[[:space:]]*:[[:space:]]*"agent/v[^"]+"' \
        | sed -E 's/.*"agent\/v([^"]+)".*/\1/' \
        | sort -V \
        | tail -n1
    )"
  fi

  [[ -n "$tag" ]] || die "could not determine latest agent release version"
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

  log "Verifying checksum"
  curl -fsSL "$checksums_url" -o "${tmpdir}/checksums.txt"
  (
    cd "$tmpdir"
    grep " ${asset}\$" checksums.txt | sha256sum -c - >&2
  )

  DOWNLOADED="${tmpdir}/${asset}"
}

config_template_path() {
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if [[ -f "${script_dir}/config-example.yml" ]]; then
    echo "${script_dir}/config-example.yml"
    return 0
  fi
  return 1
}

download_config_template() {
  local version="$1"
  local dest="$2"
  local tag="agent/v${version}"
  local base_url="https://github.com/${GITHUB_REPO}/releases/download/${tag}"
  local template_url="${base_url}/config-example.yml"

  if path="$(config_template_path)"; then
    cp "$path" "$dest"
    return 0
  fi

  log "Downloading config-example.yml (${tag})"
  if curl -fsSL "$template_url" -o "$dest"; then
    return 0
  fi

  log "config-example.yml not in release, using main branch template"
  curl -fsSL "https://raw.githubusercontent.com/${GITHUB_REPO}/master/${AGENT_PATH}/config-example.yml" -o "$dest"
}

write_config() {
  local ingest_token="$1"
  local api_endpoint="$2"
  local template="$3"
  local token_escaped endpoint_escaped

  [[ -f "$template" ]] || die "config template not found: ${template}"

  token_escaped="$(escape_yaml_string "$ingest_token")"
  endpoint_escaped="$(escape_yaml_string "$api_endpoint")"

  install -d -m 0755 "$CONFIG_DIR"
  sed -e "s|^ingest_token:.*|ingest_token: \"${token_escaped}\"|" \
      -e "s|^api_endpoint:.*|api_endpoint: \"${endpoint_escaped}\"|" \
      "$template" >"$CONFIG_FILE"
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

remove_file() {
  local path="$1"
  local label="$2"

  if [[ -e "$path" ]]; then
    log "Removing ${label}: ${path}"
    rm -f "$path"
  fi
}

uninstall() {
  log "Uninstalling monitor-agent"

  if command -v systemctl >/dev/null 2>&1; then
    systemctl disable --now "${SERVICE_NAME}-update.timer" 2>/dev/null || true
    systemctl disable --now "$SERVICE_NAME" 2>/dev/null || true
    systemctl stop "${SERVICE_NAME}-update.timer" 2>/dev/null || true
    systemctl stop "$SERVICE_NAME" 2>/dev/null || true
  fi

  remove_file "/etc/systemd/system/${SERVICE_NAME}-update.timer" "update timer"
  remove_file "/etc/systemd/system/${SERVICE_NAME}-update.service" "update service"
  remove_file "$SERVICE_FILE" "systemd service"

  if command -v systemctl >/dev/null 2>&1; then
    systemctl daemon-reload 2>/dev/null || true
  fi

  remove_file "$INSTALL_BIN" "binary"
  remove_file "${INSTALL_BIN}.old" "binary backup"
  if [[ -d "$CONFIG_DIR" ]]; then
    log "Removing config directory: ${CONFIG_DIR}"
    rm -rf "$CONFIG_DIR"
  fi
  remove_file "/var/log/monitor-agent.log" "log file"

  log "Uninstall complete"
}

install_agent() {
  require_systemd

  ARCH="$(detect_arch)"
  TMPDIR="$(mktemp -d)"
  trap 'rm -rf "$TMPDIR"' EXIT

  if [[ -z "$VERSION" ]]; then
    VERSION="$(fetch_latest_version)"
  fi

  DOWNLOADED=""
  download_release "$VERSION" "$ARCH" "$TMPDIR"
  [[ -f "$DOWNLOADED" ]] || die "failed to download monitor-agent binary"

  log "Installing binary to ${INSTALL_BIN}"
  install -d -m 0755 "$(dirname "$INSTALL_BIN")"
  install -m 0755 "$DOWNLOADED" "$INSTALL_BIN"

  CONFIG_TEMPLATE="${TMPDIR}/config-example.yml"
  download_config_template "$VERSION" "$CONFIG_TEMPLATE"

  log "Writing config to ${CONFIG_FILE}"
  write_config "$INGEST_TOKEN" "$API_ENDPOINT" "$CONFIG_TEMPLATE"

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
}

COMMAND=""
INGEST_TOKEN=""
VERSION=""
API_ENDPOINT="$DEFAULT_API_ENDPOINT"
AUTO_UPDATE="true"

while [[ $# -gt 0 ]]; do
  case "$1" in
    install)
      COMMAND="install"
      shift
      [[ $# -ge 1 ]] || die "install requires an ingest token"
      INGEST_TOKEN="$1"
      shift
      ;;
    uninstall)
      COMMAND="uninstall"
      shift
      ;;
    --version)
      [[ "$COMMAND" == "install" ]] || die "options are only valid with install"
      [[ $# -ge 2 ]] || die "--version requires a value"
      VERSION="$2"
      shift 2
      ;;
    --api-endpoint)
      [[ "$COMMAND" == "install" ]] || die "options are only valid with install"
      [[ $# -ge 2 ]] || die "--api-endpoint requires a value"
      API_ENDPOINT="$2"
      shift 2
      ;;
    --auto-update)
      [[ "$COMMAND" == "install" ]] || die "options are only valid with install"
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
      die "unknown argument: $1"
      ;;
  esac
done

require_root
require_linux

case "$COMMAND" in
  install)
    install_agent
    ;;
  uninstall)
    uninstall
    ;;
  *)
    usage
    die "install or uninstall is required"
    ;;
esac
