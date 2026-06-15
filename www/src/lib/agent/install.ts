import { env } from "@/env/client"

export const AGENT_INSTALL_SH_URL =
  "https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.sh"

export const AGENT_INSTALL_PS1_URL =
  "https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.ps1"

export const AGENT_DOCKER_IMAGE = "ghcr.io/realfascinated/monitor-agent:latest"

export const AGENT_DOCKER_IMAGE_NVIDIA =
  "ghcr.io/realfascinated/monitor-agent:nvidia"

export type AgentInstallMethod =
  | "linux"
  | "windows"
  | "docker"
  | "docker-nvidia"
  | "unraid"
  | "unraid-nvidia"

export function getIngestApiEndpoint(): string {
  const base = env.VITE_API_URL.replace(/\/$/, "")
  return `${base}/v1/servers/ingest`
}

export function buildLinuxInstallCommand(ingestToken: string): string {
  return `curl -fsSL ${AGENT_INSTALL_SH_URL} | sudo bash -s -- install ${ingestToken}`
}

export function buildWindowsInstallCommand(ingestToken: string): string {
  return `Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr ${AGENT_INSTALL_PS1_URL} -UseBasicParsing).Content)) install ${ingestToken}`
}

export function buildDockerCompose(
  ingestToken: string,
  options: { nvidia: boolean }
): string {
  const image = options.nvidia ? AGENT_DOCKER_IMAGE_NVIDIA : AGENT_DOCKER_IMAGE
  const gpusLine = options.nvidia ? "    gpus: all\n" : ""

  return `services:
  monitor-agent:
    image: ${image}
    container_name: monitor-agent
    restart: unless-stopped
    privileged: true
    pid: host
    network_mode: host
${gpusLine}    environment:
      MONITOR_CONFIG_FILE: "-"
      MONITOR_INGEST_TOKEN: ${ingestToken}
      MONITOR_API_ENDPOINT: ${getIngestApiEndpoint()}
      MONITOR_PUSH_SCHEDULE: "*/15 * * * * *"
      MONITOR_ENABLE_DOCKER: "true"
      MONITOR_ENABLE_GPU: "true"
      MONITOR_HOST_ROOT: /host
    volumes:
      - /:/host:ro,rslave
      - /proc:/proc:ro
      - /sys:/sys:ro
      - /dev:/dev:ro
      - /etc/os-release:/etc/os-release:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro`
}

export function isCommandInstallMethod(method: AgentInstallMethod): boolean {
  return !method.startsWith("unraid")
}

export function getUnraidInstallSteps(nvidia: boolean): string[] {
  const appName = nvidia ? "monitor-agent-nvidia" : "monitor-agent"

  return [
    `Open Apps and search for "${appName}".`,
    "Install the container from Community Applications.",
    "Paste your ingest token into the Ingest Token field.",
    "Apply to start the container.",
  ]
}

export function getAgentInstallContent(
  method: AgentInstallMethod,
  ingestToken: string
): string {
  switch (method) {
    case "linux":
      return buildLinuxInstallCommand(ingestToken)
    case "windows":
      return buildWindowsInstallCommand(ingestToken)
    case "docker":
      return buildDockerCompose(ingestToken, { nvidia: false })
    case "docker-nvidia":
      return buildDockerCompose(ingestToken, { nvidia: true })
    case "unraid":
    case "unraid-nvidia":
      throw new Error(`No install command for method: ${method}`)
  }
}
