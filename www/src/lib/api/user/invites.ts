import { apiFetch } from "@/lib/auth/api"

import type { ServerRole } from "@/lib/api/user/servers"

export type UserPendingInvite = {
  inviteId: number
  serverId: number
  serverName: string
  role: ServerRole
  invitedByEmail: string
  expiresAt: string
  createdAt: string
}

export type ServerInvitePreview = {
  serverName: string
  role: ServerRole
  email: string
  invitedByEmail: string
  expiresAt: string
}

export type ServerInviteAcceptRequest = {
  token: string
}

export type ServerMemberResponse = {
  serverId: number
  serverName: string
  role: ServerRole
  joinedAt: string
}

export function getUserPendingInvites(): Promise<UserPendingInvite[]> {
  return apiFetch<UserPendingInvite[]>("/v1/user/invites")
}

export function getServerInvitePreview(
  token: string
): Promise<ServerInvitePreview> {
  const params = new URLSearchParams({ token })
  return apiFetch<ServerInvitePreview>(`/v1/user/invites/preview?${params}`)
}

export function acceptServerInvite(
  request: ServerInviteAcceptRequest
): Promise<ServerMemberResponse> {
  return apiFetch<ServerMemberResponse>("/v1/user/invites/accept", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function acceptServerInviteById(
  inviteId: number
): Promise<ServerMemberResponse> {
  return apiFetch<ServerMemberResponse>(`/v1/user/invites/${inviteId}/accept`, {
    method: "POST",
  })
}
