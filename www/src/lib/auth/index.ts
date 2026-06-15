import { ApiClientError, apiFetch } from "@/lib/auth/api"
import { getMe } from "@/lib/auth/session"
import type { AuthToken, Credentials, User } from "@/lib/auth/types"
import { clearToken, setSession } from "@/lib/auth/token"

export type {
  ApiError,
  AuthToken,
  Credentials,
  User,
  UserRole,
} from "@/lib/auth/types"
export { ApiClientError, apiFetch } from "@/lib/auth/api"
export { AuthProvider, useAuth } from "@/lib/auth/context"
export { fetchCurrentUser, getMe } from "@/lib/auth/session"
export { requireAdmin } from "@/lib/auth/require-admin"
export { validateCredentials } from "@/lib/auth/validation"

export type AuthResult = { error: string } | { user: User }

async function authenticate(
  path: "/v1/auth/login" | "/v1/auth/register",
  credentials: Credentials
): Promise<AuthResult> {
  try {
    const result = await apiFetch<AuthToken>(path, {
      method: "POST",
      body: JSON.stringify(credentials),
      auth: false,
    })

    setSession(result.token, result.expiresAt)
    const user = await getMe()
    return { user }
  } catch (error) {
    if (
      error instanceof ApiClientError &&
      (error.status === 401 || error.status === 403 || error.status === 409)
    ) {
      return { error: error.message }
    }

    throw error
  }
}

export function login(credentials: Credentials): Promise<AuthResult> {
  return authenticate("/v1/auth/login", credentials)
}

export function register(credentials: Credentials): Promise<AuthResult> {
  return authenticate("/v1/auth/register", credentials)
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<void>("/v1/auth/logout", { method: "POST" })
  } catch {
    // Best-effort server revocation; always clear local storage.
  }

  clearToken()
}
