import { apiFetch } from "@/lib/auth/api"

export type ApiToken = {
  id: number
  name: string
  prefix: string
  scopes: string[]
  createdAt: string
  expiresAt: string | null
  lastUsedAt: string | null
}

export type ApiTokenScope = {
  id: string
  description: string
}

export type CreatedApiToken = {
  token: string
  info: ApiToken
}

export type CreateApiTokenRequest = {
  name: string
  scopes: string[]
  expiresInDays: number | null
}

export function listApiTokens(): Promise<ApiToken[]> {
  return apiFetch<ApiToken[]>("/v1/user/api-tokens")
}

export function listApiTokenScopes(): Promise<ApiTokenScope[]> {
  return apiFetch<ApiTokenScope[]>("/v1/user/api-tokens/scopes")
}

export function createApiToken(
  request: CreateApiTokenRequest
): Promise<CreatedApiToken> {
  return apiFetch<CreatedApiToken>("/v1/user/api-tokens", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function revokeApiToken(tokenId: number): Promise<void> {
  return apiFetch<void>(`/v1/user/api-tokens/${tokenId}`, {
    method: "DELETE",
  })
}
