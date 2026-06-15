import { Outlet, createFileRoute } from "@tanstack/react-router"

import { resolveUserServerFromCache } from "@/lib/api/user/servers.queries"
import { serverPageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/servers/$serverId")({
  ssr: false,
  loader: ({ context, params }) => {
    const serverId = Number(params.serverId)
    return resolveUserServerFromCache(context.queryClient, serverId)
  },
  head: ({ loaderData }) => ({
    meta: [{ title: serverPageTitle(loaderData) }],
  }),
  component: ServerLayout,
})

function ServerLayout() {
  return <Outlet />
}
