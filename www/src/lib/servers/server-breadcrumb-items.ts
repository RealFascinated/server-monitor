import type { BreadcrumbItem } from "@/components/breadcrumb"
import type { ServerResponse } from "@/lib/api/user/servers"
import type { MetricTimeRange } from "@/lib/metrics/range"

function serverBreadcrumbItems(
  server: ServerResponse | undefined,
  serverId: number,
  defaultRange: MetricTimeRange,
  tail?: BreadcrumbItem
): BreadcrumbItem[] {
  const serverLabel = server?.serverName ?? `Server ${serverId}`
  const items: BreadcrumbItem[] = [{ label: "Servers", to: "/" }]

  if (server?.folderName) {
    items.push({ label: server.folderName, to: "/" })
  }

  if (tail) {
    items.push(
      {
        label: serverLabel,
        to: "/servers/$serverId",
        params: { serverId: String(serverId) },
        search: { range: defaultRange },
      },
      tail
    )
  } else {
    items.push({ label: serverLabel, current: true })
  }

  return items
}

export { serverBreadcrumbItems }
