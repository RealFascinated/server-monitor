import { Outlet, createFileRoute, notFound } from "@tanstack/react-router"
import type { ErrorComponentProps } from "@tanstack/react-router"

import { Callout } from "@/components/callout"
import { ServerNotFoundView } from "@/components/server/server-not-found-view"
import { Button } from "@/components/ui/button"
import { resolveUserServer } from "@/lib/api/user/servers.queries"
import { getApiErrorMessage, getApiErrorTitle } from "@/lib/api/error-message"
import { ApiClientError } from "@/lib/auth/api"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle, serverPageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/servers/$serverId")({
  ssr: false,
  loader: async ({ context, params }) => {
    const serverId = Number(params.serverId)
    if (!Number.isFinite(serverId)) {
      throw notFound()
    }

    try {
      return await resolveUserServer(context.queryClient, serverId)
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 404) {
        throw notFound()
      }
      throw error
    }
  },
  head: ({ loaderData }) => ({
    meta: [
      {
        title: loaderData
          ? serverPageTitle(loaderData)
          : pageTitle("Server not found"),
      },
    ],
  }),
  notFoundComponent: ServerNotFoundRoute,
  errorComponent: ServerRouteError,
  component: ServerLayout,
})

function ServerNotFoundRoute() {
  const { serverId } = Route.useParams()
  return <ServerNotFoundView serverId={serverId} />
}

function ServerRouteError({ error, reset }: ErrorComponentProps) {
  return (
    <section className={authenticatedPageSectionClassName}>
      <Callout
        type="danger"
        title={getApiErrorTitle(error, "Could not load server")}
      >
        <div className="flex flex-col gap-3">
          <p>{getApiErrorMessage(error, "Failed to load server.")}</p>
          <div>
            <Button type="button" variant="outline" onClick={reset}>
              Try again
            </Button>
          </div>
        </div>
      </Callout>
    </section>
  )
}

function ServerLayout() {
  return <Outlet />
}
