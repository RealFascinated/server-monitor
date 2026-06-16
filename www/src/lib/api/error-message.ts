import { ApiClientError } from "@/lib/auth/api"
import { NetworkError } from "@/lib/network"

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof NetworkError || error instanceof ApiClientError) {
    return error.message
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

export function getApiErrorTitle(error: unknown, fallback: string): string {
  if (error instanceof NetworkError) {
    return error.title
  }

  return fallback
}
