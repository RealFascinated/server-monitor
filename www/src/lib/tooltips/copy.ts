import type { ServerStatus, ServerRole } from "@/lib/api/user/servers"
import type { UserRole } from "@/lib/auth/types"

export const PENDING_NO_METRIC_DATA =
  "No data yet. The agent may be offline or still connecting."

export function pendingOnlyTooltip(status: ServerStatus): string | null {
  return status === "PENDING" ? PENDING_NO_METRIC_DATA : null
}

export const SERVER_STATUS_TOOLTIPS: Record<ServerStatus, string> = {
  ONLINE: "The agent reported metrics recently.",
  OFFLINE: "No recent heartbeat from the agent.",
  PENDING: "Server created, but the agent has not connected yet.",
}

export const SERVER_TABLE_COLUMN_TOOLTIPS = {
  uptime:
    "Time since the host last booted, based on agent-reported uptime. Shows — when the agent is offline.",
  uptime30d:
    "Percentage of time the agent reported as online over the last 30 days.",
  cpu: "Host-wide CPU utilization across all cores. Shows — when no recent data is available.",
  memory:
    "Physical memory in use as a percentage of total RAM. Hover a value for exact bytes.",
  rootDisk:
    "Root filesystem usage as a percentage of total capacity. Hover a value for exact bytes.",
  agent:
    "Monitor Agent version installed on the host. Shows — when the agent has not reported a version.",
  created: "When this server was registered in Monitor.",
} as const

export const USER_ROLE_TOOLTIPS: Record<UserRole, string> = {
  ADMIN: "Can manage all servers and users across the account.",
  USER: "Can access servers shared with this account.",
}

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: "Admin",
  USER: "User",
}

export const SERVER_ROLE_TOOLTIPS: Record<ServerRole | "OWNER", string> = {
  OWNER: "Full control: rename, delete, rotate tokens, and manage access.",
  VIEWER: "Read-only access to this server's metrics and settings view.",
}

export const SETTINGS_TOOLTIPS = {
  registrationEnabled:
    "Allow new users to create accounts via the public registration page.",
  ingestToken:
    "Authenticates the Monitor Agent on this host. Shown once after rotation — copy it before closing.",
  rotateIngestToken:
    "Issues a new token and revokes the previous one immediately. Update the agent configuration afterward.",
  deleteServer:
    "Permanently deletes this server, all stored metrics, access grants, and pending invites.",
  leaveServer:
    "Removes your viewer access to this server. You can rejoin if the owner sends a new invite.",
} as const

export const INVITE_EXPIRY_TOOLTIP =
  "Invite expires on this date. Accept before then or ask the owner to resend."

export const INCIDENT_STATUS_TOOLTIPS = {
  ongoing: "The agent stopped reporting heartbeats. This outage is ongoing.",
  resolved: "The agent came back online and this outage was closed.",
} as const
