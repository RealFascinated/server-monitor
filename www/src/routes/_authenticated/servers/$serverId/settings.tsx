import { createFileRoute } from "@tanstack/react-router"

import { AnimatedContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { LoadingState } from "@/components/loading-state"
import { ServerSettingsHeader } from "@/components/server/server-settings-header"
import { ServerSettingsView } from "@/components/server/server-settings-view"
import { useServerAccess } from "@/hooks/use-server-access"
import { useUserServer } from "@/hooks/use-user-server"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import type { ServerResponse } from "@/lib/api/user/servers"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { serverPageTitle } from "@/lib/page-title"

export const Route = createFileRoute(
  "/_authenticated/servers/$serverId/settings"
)({
  head: ({ matches }) => {
    const servers = matches.find(
      (match) =>
        (match.routeId as string) === "/_authenticated/servers/$serverId"
    )?.loaderData as ServerResponse | undefined

    return {
      meta: [{ title: serverPageTitle(servers, "Settings") }],
    }
  },
  component: ServerSettingsPage,
})

function ServerSettingsPage() {
  const { serverId } = Route.useParams()
  const numericServerId = Number(serverId)

  const { data: server, isPending: serverPending } =
    useUserServer(numericServerId)
  const canListMembers =
    server !== undefined &&
    hasPermission(server.permissions, ServerPermission.LIST_MEMBERS)

  const {
    data: access,
    isPending: accessPending,
    error,
  } = useServerAccess(numericServerId, canListMembers)

  const errorMessage = error instanceof Error ? error.message : null
  const isPending = serverPending || (canListMembers && accessPending)

  return (
    <section className={authenticatedPageSectionClassName}>
      <ServerSettingsHeader server={server} serverId={numericServerId} />

      {errorMessage ? (
        <Callout type="danger" title="Could not load settings">
          {errorMessage}
        </Callout>
      ) : null}

      {isPending && !errorMessage ? (
        <LoadingState message="Loading settings…" />
      ) : null}

      {!errorMessage && server && (!canListMembers || access) ? (
        <AnimatedContent>
          <ServerSettingsView
            serverId={numericServerId}
            server={server}
            access={access}
          />
        </AnimatedContent>
      ) : null}
    </section>
  )
}
