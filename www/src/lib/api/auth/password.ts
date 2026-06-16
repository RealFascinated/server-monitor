import { apiFetch } from "@/lib/auth/api"

export type ForgotPasswordRequest = {
  email: string
}

export type ResetPasswordRequest = {
  token: string
  password: string
}

export function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  return apiFetch<void>("/v1/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify(data),
    auth: false,
  })
}

export function resetPassword(data: ResetPasswordRequest): Promise<void> {
  return apiFetch<void>("/v1/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(data),
    auth: false,
  })
}
