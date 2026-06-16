import type { ReactNode } from "react"

import type { BreadcrumbItem } from "@/components/breadcrumb"
import { PageHeader } from "@/components/page-header"
import { serverBreadcrumbItems } from "@/components/server/server-breadcrumb-items"
import { ServerMetaSubtitle } from "@/components/server/server-meta-subtitle"
import { ServerStatusBadge } from "@/components/server/server-status-badge"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import type { ServerResponse } from "@/lib/api/user/servers"

type ServerDetailHeaderProps = {
  server: ServerResponse | undefined
  serverId: number
  breadcrumbTail?: BreadcrumbItem
  actions?: ReactNode
  footer?: ReactNode
}

function ServerDetailHeader({
  server,
  serverId,
  breadcrumbTail,
  actions,
  footer,
}: ServerDetailHeaderProps) {
  const { defaultRange } = useMetricDefaultRange()

  return (
    <PageHeader
      breadcrumb={serverBreadcrumbItems(
        server,
        serverId,
        defaultRange,
        breadcrumbTail
      )}
      title={server?.serverName ?? `Server ${serverId}`}
      titleAddon={server ? <ServerStatusBadge status={server.status} /> : null}
      subtitle={server ? <ServerMetaSubtitle server={server} /> : null}
      actions={actions}
      footer={footer}
    />
  )
}

export { ServerDetailHeader }
