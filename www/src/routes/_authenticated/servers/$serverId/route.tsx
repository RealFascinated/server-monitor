import { Outlet, createFileRoute, notFound } from "@tanstack/react-router"

import { ServerNotFoundView } from "@/components/server/server-not-found-view"
import { resolveUserServer } from "@/lib/api/user/servers.queries"
import { ApiClientError } from "@/lib/auth/api"
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
  component: ServerLayout,
})

function ServerNotFoundRoute() {
  const { serverId } = Route.useParams()
  return <ServerNotFoundView serverId={serverId} />
}

function ServerLayout() {
  return <Outlet />
}
