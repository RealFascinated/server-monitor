import { useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"

import { Callout } from "@/components/callout"
import { QueryStatusShell } from "@/components/query-status-shell"
import { ServerIncidentsHeader } from "@/components/server/server-incidents-header"
import { ServerIncidentsView } from "@/components/server/server-incidents-view"
import { useUserServer } from "@/hooks/use-user-server"
import { serverIncidentsQueryOptions } from "@/lib/api/user/incidents.queries"
import { loadCachedQuery } from "@/lib/api/query-loader"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import type { ServerResponse } from "@/lib/api/user/servers"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { serverPageTitle } from "@/lib/page-title"
import { pageSearchSchema } from "@/lib/schemas/pagination"

const incidentsSearchSchema = pageSearchSchema()

export const Route = createFileRoute(
  "/_authenticated/servers/$serverId/incidents"
)({
  validateSearch: incidentsSearchSchema,
  loaderDeps: ({ search }) => ({ pagination: search }),
  loader: ({ context: { queryClient }, params, deps: { pagination } }) => {
    const serverId = Number(params.serverId)
    return loadCachedQuery(
      queryClient,
      serverIncidentsQueryOptions(serverId, pagination)
    )
  },
  head: ({ matches }) => {
    const servers = matches.find(
      (match) =>
        (match.routeId as string) === "/_authenticated/servers/$serverId"
    )?.loaderData as ServerResponse | undefined

    return {
      meta: [{ title: serverPageTitle(servers, "Incidents") }],
    }
  },
  component: ServerIncidentsPage,
})

function ServerIncidentsPage() {
  const { serverId } = Route.useParams()
  const pagination = Route.useSearch()
  const navigate = useNavigate()
  const numericServerId = Number(serverId)

  const { data: server } = useUserServer(numericServerId)
  const canViewIncidents =
    server !== undefined &&
    hasPermission(server.permissions, ServerPermission.VIEW_SERVER)

  const {
    data: incidentsPage,
    isPending,
    error,
  } = useQuery({
    ...serverIncidentsQueryOptions(numericServerId, pagination),
    enabled: canViewIncidents,
  })

  function handlePageChange(page: number) {
    navigate({
      to: "/servers/$serverId/incidents",
      params: { serverId: String(numericServerId) },
      search: { ...pagination, page },
      resetScroll: false,
    })
  }

  function handlePageSizeChange(count: number) {
    navigate({
      to: "/servers/$serverId/incidents",
      params: { serverId: String(numericServerId) },
      search: { page: 1, count },
      resetScroll: false,
    })
  }

  return (
    <section className={authenticatedPageSectionClassName}>
      <ServerIncidentsHeader server={server} serverId={numericServerId} />

      <div className="flex flex-col gap-6">
        {!canViewIncidents && server ? (
          <Callout type="danger" title="Access denied">
            You do not have permission to view incidents for this server.
          </Callout>
        ) : null}

        <QueryStatusShell
          error={canViewIncidents ? error : null}
          isPending={canViewIncidents && isPending}
          loadingMessage="Loading incidents…"
          fallbackMessage="Failed to load incidents"
          fallbackTitle="Could not load incidents"
        >
          {canViewIncidents && incidentsPage ? (
            <ServerIncidentsView
              page={incidentsPage}
              pagination={pagination}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
            />
          ) : null}
        </QueryStatusShell>
      </div>
    </section>
  )
}
