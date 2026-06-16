import type {
  DefaultError,
  FetchQueryOptions,
  QueryClient,
  QueryKey,
} from "@tanstack/react-query"

/**
 * Non-blocking route loader for query-backed pages.
 *
 * Returns cached data immediately so navigation can finish and the page shell
 * (headers, layout) renders without waiting on the network. Starts a
 * background prefetch when nothing is cached; the route component should read
 * the same query with `useQuery` and handle `isPending` / errors there.
 *
 * Use for heavy or frequently re-fetched data such as metrics and paginated
 * lists where a loading state inside the page is acceptable.
 *
 * Prefer `queryClient.ensureQueryData` when the route must block until data
 * is available — e.g. settings forms, auth gates, or parent loaders that
 * other routes depend on (see `resolveUserServer`).
 */
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
