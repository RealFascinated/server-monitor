import { apiFetch } from "@/lib/auth/api"

export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
}

export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return apiFetch<void>("/v1/user/password", {
    method: "PATCH",
    body: JSON.stringify(data),
  })
}
