import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useCallback, useDeferredValue, useEffect } from "react"

import { Callout } from "@/components/callout"
import { QueryStatusShell } from "@/components/query-status-shell"
import { ServerAgentSetupDialog } from "@/components/server/server-agent-setup-dialog"
import { ServerMetricsHeader } from "@/components/server/server-metrics-header"
import { ServerMetricsView } from "@/components/server/server-metrics-view"
import { ServerOfflineBanner } from "@/components/server/server-offline-banner"
import { useUserServer } from "@/hooks/use-user-server"
import { useMetricRefreshInterval } from "@/hooks/use-metric-refresh-interval"
import { userServerMetricsQueryOptions } from "@/lib/api/user/metrics.queries"
import { loadCachedQuery } from "@/lib/api/query-loader"
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

  const metricsForServer = metrics?.id === numericServerId ? metrics : undefined
  const deferredMetrics = useDeferredValue(metricsForServer)
  const readyMetrics =
    deferredServerId === numericServerId &&
    deferredMetrics?.id === numericServerId
      ? deferredMetrics
      : undefined

  const showLoading =
    (isPending && !metricsForServer) ||
    (metricsForServer != null && !readyMetrics)

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
        hasPermission(
          server.permissions,
          ServerPermission.ROTATE_INGEST_TOKEN
        ) ? (
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

        <QueryStatusShell
          error={error}
          isPending={showLoading}
          loadingMessage="Loading metrics…"
          fallbackMessage="Failed to load server metrics"
          fallbackTitle="Could not load metrics"
        >
          {readyMetrics ? (
            <ServerMetricsView
              metrics={readyMetrics}
              timeWindow={timeWindow}
              dataWindow={{ from: readyMetrics.from, to: readyMetrics.to }}
              onZoomToRange={handleZoomToRange}
              zoomDisabled={isFetching}
            />
          ) : null}
        </QueryStatusShell>
      </div>
    </section>
  )
}
