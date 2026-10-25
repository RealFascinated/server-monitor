import { queryOptions } from "@tanstack/react-query"

import { listApiTokenScopes, listApiTokens } from "@/lib/api/user/api-tokens"

export const apiTokensQueryKey = ["user", "api-tokens"] as const

export function apiTokensQueryOptions() {
  return queryOptions({
    queryKey: apiTokensQueryKey,
    queryFn: listApiTokens,
  })
}

export function apiTokenScopesQueryOptions() {
  return queryOptions({
    queryKey: [...apiTokensQueryKey, "scopes"] as const,
    queryFn: listApiTokenScopes,
    staleTime: Infinity,
  })
}
