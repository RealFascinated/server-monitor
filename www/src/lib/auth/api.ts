import { env } from "@/env/client"
import type { ApiError } from "@/lib/auth/types"
import { clearToken, getToken } from "@/lib/auth/token"

export class ApiClientError extends Error {
  readonly status: number
  readonly code: number

  constructor(message: string, status: number, code: number) {
    super(message)
    this.name = "ApiClientError"
    this.status = status
    this.code = code
  }
}

type ApiFetchOptions = RequestInit & {
  auth?: boolean
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { auth = true, ...init } = options
  const headers = new Headers(init.headers)

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }

  if (auth) {
    const token = getToken()
    if (token) {
      headers.set("Authorization", `Bearer ${token}`)
    }
  }

  const response = await fetch(`${env.VITE_API_URL}${path}`, {
    ...init,
    headers,
  })

  if (!response.ok) {
    let message = response.statusText
    let code = response.status

    try {
      const error = (await response.json()) as ApiError
      if (error.message) {
        message = error.message
      }
      if (error.code) {
        code = error.code
      }
    } catch {
      // Response may have no JSON body.
    }

    if (response.status === 401 && auth) {
      clearToken()
    }

    throw new ApiClientError(message, response.status, code)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }

  return JSON.parse(text) as T
}
