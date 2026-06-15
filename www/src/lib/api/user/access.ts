import { apiFetch } from "@/lib/auth/api"
import type { ServerRole } from "@/lib/api/user/servers"

export type ServerAccessOwner = {
  id: number
  email: string
}

export type ServerAccessMember = {
  userId: number
  email: string
  role: ServerRole
  joinedAt: string
}

export type PendingServerInvite = {
  inviteId: number
  email: string
  role: ServerRole
  expiresAt: string
  createdAt: string
}

export type ServerAccessListResponse = {
  owner: ServerAccessOwner
  members: ServerAccessMember[]
  pendingInvites: PendingServerInvite[]
}

export type ServerMemberInviteRequest = {
  email: string
}

export type ServerInviteCreatedResponse = {
  inviteId: number
  email: string
  role: ServerRole
  expiresAt: string
  token: string
}

export function getServerAccess(
  serverId: number
): Promise<ServerAccessListResponse> {
  return apiFetch<ServerAccessListResponse>(`/v1/servers/${serverId}/members`)
}

export function inviteServerMember(
  serverId: number,
  request: ServerMemberInviteRequest
): Promise<ServerInviteCreatedResponse> {
  return apiFetch<ServerInviteCreatedResponse>(
    `/v1/servers/${serverId}/members/invite`,
    {
      method: "POST",
      body: JSON.stringify(request),
    }
  )
}

export function removeServerMember(
  serverId: number,
  memberUserId: number
): Promise<void> {
  return apiFetch<void>(`/v1/servers/${serverId}/members/${memberUserId}`, {
    method: "DELETE",
  })
}

export function leaveServer(serverId: number): Promise<void> {
  return apiFetch<void>(`/v1/servers/${serverId}/leave`, {
    method: "DELETE",
  })
}

export function revokeServerInvite(
  serverId: number,
  inviteId: number
): Promise<void> {
  return apiFetch<void>(`/v1/servers/${serverId}/invites/${inviteId}`, {
    method: "DELETE",
  })
}
