import { apiFetch } from "@/lib/auth/api"

export type UserSession = {
  id: number
  createdAt: string
  expiresAt: string
  current: boolean
}

export function listUserSessions(): Promise<UserSession[]> {
  return apiFetch<UserSession[]>("/v1/user/sessions")
}

export function revokeUserSession(sessionId: number): Promise<void> {
  return apiFetch<void>(`/v1/user/sessions/${sessionId}`, {
    method: "DELETE",
  })
}

export function revokeOtherUserSessions(): Promise<void> {
  return apiFetch<void>("/v1/user/sessions/others", {
    method: "DELETE",
  })
}
