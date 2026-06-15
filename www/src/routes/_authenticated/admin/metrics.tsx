import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useCallback } from "react"

import { AdminMetricsHeader } from "@/components/admin/admin-metrics-header"
import { AdminMetricsView } from "@/components/admin/admin-metrics-view"
import { AnimatedContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { LoadingState } from "@/components/loading-state"
import { adminMetricsQueryOptions } from "@/lib/api/admin/metrics.queries"
import { useMetricRefreshInterval } from "@/hooks/use-metric-refresh-interval"
import { ApiClientError } from "@/lib/auth/api"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle } from "@/lib/page-title"
import { metricRangeSearchSchema } from "@/lib/schemas/range"

const adminMetricsSearchSchema = metricRangeSearchSchema()

export const Route = createFileRoute("/_authenticated/admin/metrics")({
  ssr: false,
  validateSearch: adminMetricsSearchSchema,
  loaderDeps: ({ search }) => ({ timeWindow: search }),
  loader: ({ context: { queryClient }, deps: { timeWindow } }) => {
    return queryClient.ensureQueryData(adminMetricsQueryOptions(timeWindow))
  },
  head: () => ({
    meta: [{ title: pageTitle("Admin Metrics") }],
  }),
  component: AdminMetricsPage,
})

function AdminMetricsPage() {
  const timeWindow = Route.useSearch()
  const navigate = useNavigate()
  const { refreshInterval, setRefreshInterval } = useMetricRefreshInterval()

  const {
    data: metrics,
    isPending,
    isFetching,
    refetch,
    error,
  } = useQuery(adminMetricsQueryOptions(timeWindow, refreshInterval))

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : error
        ? "Failed to load admin metrics"
        : null

  const handleZoomToRange = useCallback(
    (from: number, to: number) => {
      navigate({
        to: "/admin/metrics",
        search: { from, to },
        resetScroll: false,
      })
    },
    [navigate]
  )

  const dataWindow = metrics ? { from: metrics.from, to: metrics.to } : null

  return (
    <section className={authenticatedPageSectionClassName}>
      <AdminMetricsHeader
        timeWindow={timeWindow}
        refreshInterval={refreshInterval}
        onRefreshIntervalChange={setRefreshInterval}
        onRefresh={() => void refetch()}
        isRefreshing={isFetching}
      />

      {errorMessage ? (
        <Callout type="danger" title="Could not load metrics">
          {errorMessage}
        </Callout>
      ) : null}

      {isPending && !errorMessage ? (
        <LoadingState message="Loading metrics…" />
      ) : null}

      {metrics && dataWindow && !errorMessage ? (
        <AnimatedContent>
          <AdminMetricsView
            metrics={metrics}
            timeWindow={timeWindow}
            dataWindow={dataWindow}
            onZoomToRange={handleZoomToRange}
            zoomDisabled={isFetching}
          />
        </AnimatedContent>
      ) : null}
    </section>
  )
}
