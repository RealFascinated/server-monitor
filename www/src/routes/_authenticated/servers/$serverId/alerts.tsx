import { createFileRoute } from "@tanstack/react-router"

import { Callout } from "@/components/callout"
import { ServerAlertsView } from "@/components/server/server-alerts-view"
import { ServerDetailHeader } from "@/components/server/server-detail-header"
import { useUserServer } from "@/hooks/use-user-server"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import type { ServerResponse } from "@/lib/api/user/servers"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { serverPageTitle } from "@/lib/page-title"

export const Route = createFileRoute(
  "/_authenticated/servers/$serverId/alerts"
)({
  head: ({ matches }) => {
    const servers = matches.find(
      (match) =>
        (match.routeId as string) === "/_authenticated/servers/$serverId"
    )?.loaderData as ServerResponse | undefined

    return { meta: [{ title: serverPageTitle(servers, "Alerts") }] }
  },
  component: ServerAlertsPage,
})

function ServerAlertsPage() {
  const { serverId } = Route.useParams()
  const numericServerId = Number(serverId)
  const { data: server } = useUserServer(numericServerId)
  const canViewAlerts =
    server !== undefined &&
    hasPermission(server.permissions, ServerPermission.VIEW_METRICS)

  return (
    <section className={authenticatedPageSectionClassName}>
      <ServerDetailHeader
        server={server}
        serverId={numericServerId}
        breadcrumbTail={{ label: "Alerts", current: true }}
      />

      {!canViewAlerts && server ? (
        <Callout type="danger" title="Access denied">
          You need permission to view metrics to set up alerts for this server.
        </Callout>
      ) : null}

      {canViewAlerts ? <ServerAlertsView serverId={numericServerId} /> : null}
    </section>
  )
}
