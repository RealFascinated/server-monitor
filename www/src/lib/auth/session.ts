import { ApiClientError, apiFetch } from "@/lib/auth/api"
import type { User } from "@/lib/auth/types"
import { clearToken } from "@/lib/auth/token"

export async function getMe(): Promise<User> {
  return apiFetch<User>("/v1/auth/@me")
}

export async function fetchCurrentUser(): Promise<User | null> {
  try {
    return await getMe()
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 401) {
      clearToken()
      return null
    }

    throw error
  }
}
