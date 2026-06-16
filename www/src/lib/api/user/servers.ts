import { apiFetch } from "@/lib/auth/api"

export type ServerStatus = "ONLINE" | "OFFLINE" | "PENDING"

export type ServerRole = "OWNER" | "VIEWER"

export type ServerInventory = {
  ip: string | null
  coreCount: number | null
  threadCount: number | null
  cpuModel: string | null
  socketCount: number | null
  osName: string | null
  osVersion: string | null
  kernelVersion: string | null
}

export type ServerResponse = {
  serverId: number
  serverName: string
  status: ServerStatus
  uptimeSeconds: number | null
  uptimePercent30d: number | null
  agentVersion: string | null
  createdAt: string
  cpuPercent: number | null
  memUsage: number | null
  memMax: number | null
  diskUsage: number | null
  diskMax: number | null
  role: ServerRole
  permissions: number
  folderName: string | null
  inventory: ServerInventory | null
}

export type ServerCreateRequest = {
  name: string
  folderName?: string | null
}

export type CreatedServerResponse = {
  serverId: number
  serverName: string
  ingestToken: string
}

export type IngestTokenResponse = {
  serverId: number
  ingestToken: string
}

export type ServerRenameRequest = {
  name: string
}

export type ServerStatusResponse = {
  serverId: number
  status: ServerStatus
  lastHeartbeat: string | null
  agentVersion: string | null
  hasMetrics: boolean
}

export function getUserServers(): Promise<ServerResponse[]> {
  return apiFetch<ServerResponse[]>("/v1/user/servers")
}

export function getUserServer(serverId: number): Promise<ServerResponse> {
  return apiFetch<ServerResponse>(`/v1/servers/${serverId}`)
}

export function getServerStatus(
  serverId: number
): Promise<ServerStatusResponse> {
  return apiFetch<ServerStatusResponse>(`/v1/servers/${serverId}/status`)
}

export function createServer(
  request: ServerCreateRequest
): Promise<CreatedServerResponse> {
  return apiFetch<CreatedServerResponse>("/v1/servers/create", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function renameServer(
  serverId: number,
  request: ServerRenameRequest
): Promise<ServerResponse> {
  return apiFetch<ServerResponse>(`/v1/servers/${serverId}/rename`, {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function rotateIngestToken(
  serverId: number
): Promise<IngestTokenResponse> {
  return apiFetch<IngestTokenResponse>(
    `/v1/servers/${serverId}/ingest-token/rotate`,
    { method: "POST" }
  )
}

export function deleteServer(serverId: number): Promise<void> {
  return apiFetch<void>(`/v1/servers/${serverId}`, {
    method: "DELETE",
  })
}
