import type {
  DefaultError,
  FetchQueryOptions,
  QueryClient,
  QueryKey,
} from "@tanstack/react-query"

export function loadCachedQuery<
  TQueryFnData = unknown,
  TError = DefaultError,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(
  queryClient: QueryClient,
  options: FetchQueryOptions<TQueryFnData, TError, TData, TQueryKey>
): TData | undefined {
  const cached = queryClient.getQueryData<TData>(options.queryKey)
  if (cached !== undefined) {
    return cached
  }

  void queryClient.prefetchQuery(options)
  return undefined
}
