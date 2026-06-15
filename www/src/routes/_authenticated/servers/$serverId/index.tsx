import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useCallback, useDeferredValue, useEffect } from "react"

import { LoadingState } from "@/components/loading-state"
import { Callout } from "@/components/callout"
import { ServerAgentSetupDialog } from "@/components/server/server-agent-setup-dialog"
import { ServerMetricsHeader } from "@/components/server/server-metrics-header"
import { ServerMetricsView } from "@/components/server/server-metrics-view"
import { ServerOfflineBanner } from "@/components/server/server-offline-banner"
import { useUserServer } from "@/hooks/use-user-server"
import { useMetricRefreshInterval } from "@/hooks/use-metric-refresh-interval"
import { userServerMetricsQueryOptions } from "@/lib/api/user/metrics.queries"
import { loadCachedQuery } from "@/lib/api/query-loader"
import { ApiClientError } from "@/lib/auth/api"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { metricRangeSearchSchema } from "@/lib/schemas/range"

const serverMetricsSearchSchema = metricRangeSearchSchema()

export const Route = createFileRoute("/_authenticated/servers/$serverId/")({
  validateSearch: serverMetricsSearchSchema,
  loaderDeps: ({ search }) => ({ timeWindow: search }),
  loader: ({ context: { queryClient }, params, deps: { timeWindow } }) => {
    const serverId = Number(params.serverId)
    return loadCachedQuery(
      queryClient,
      userServerMetricsQueryOptions(serverId, timeWindow)
    )
  },
  component: ServerMetricsPage,
})

function ServerMetricsPage() {
  const { serverId } = Route.useParams()
  const timeWindow = Route.useSearch()
  const navigate = useNavigate()
  const numericServerId = Number(serverId)
  const deferredServerId = useDeferredValue(numericServerId)
  const { refreshInterval, setRefreshInterval } = useMetricRefreshInterval()

  const {
    data: metrics,
    isPending,
    isFetching,
    refetch,
    error,
  } = useQuery(
    userServerMetricsQueryOptions(numericServerId, timeWindow, refreshInterval)
  )

  const { data: server } = useUserServer(numericServerId)

  const metricsForServer =
    metrics?.id === numericServerId ? metrics : undefined
  const deferredMetrics = useDeferredValue(metricsForServer)
  const metricsReady =
    deferredServerId === numericServerId &&
    deferredMetrics?.id === numericServerId

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : error
        ? "Failed to load server metrics"
        : null

  const handleZoomToRange = useCallback(
    (from: number, to: number) => {
      navigate({
        to: "/servers/$serverId",
        params: { serverId: String(numericServerId) },
        search: { from, to },
        resetScroll: false,
      })
    },
    [navigate, numericServerId]
  )

  const dataWindow = deferredMetrics
    ? { from: deferredMetrics.from, to: deferredMetrics.to }
    : null
  const showLoading =
    (isPending && !metricsForServer && !errorMessage) ||
    (metricsForServer != null && !metricsReady)

  useEffect(() => {
    window.scrollTo({ top: 0 })
  }, [numericServerId])

  return (
    <section className={authenticatedPageSectionClassName}>
      <ServerMetricsHeader
        server={server}
        timeWindow={timeWindow}
        serverId={numericServerId}
        refreshInterval={refreshInterval}
        onRefreshIntervalChange={setRefreshInterval}
        onRefresh={() => void refetch()}
        isRefreshing={isFetching}
      />

      <div className="flex flex-col gap-6">
        {server ? (
          <ServerOfflineBanner
            serverId={numericServerId}
            status={server.status}
          />
        ) : null}

        {server?.status === "PENDING" &&
        hasPermission(server.permissions, ServerPermission.ROTATE_INGEST_TOKEN) ? (
          <Callout type="info" title="Waiting for the agent">
            <div className="flex flex-col gap-3">
              <p>
                This server has not received metrics yet. Install the Monitor
                Agent on your host to start reporting.
              </p>
              <div>
                <ServerAgentSetupDialog
                  serverId={numericServerId}
                  serverName={server.serverName}
                />
              </div>
            </div>
          </Callout>
        ) : null}

        {errorMessage ? (
          <Callout type="danger" title="Could not load metrics">
            {errorMessage}
          </Callout>
        ) : null}

        {showLoading ? (
          <LoadingState message="Loading metrics…" />
        ) : null}

        {metricsReady && deferredMetrics && dataWindow && !errorMessage ? (
          <ServerMetricsView
            metrics={deferredMetrics}
            timeWindow={timeWindow}
            dataWindow={dataWindow}
            onZoomToRange={handleZoomToRange}
            zoomDisabled={isFetching}
          />
        ) : null}
      </div>
    </section>
  )
}
