import { Link } from "@tanstack/react-router"
import { History, LineChart, Settings } from "lucide-react"
import type { LucideIcon } from "lucide-react"

import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"

const tabClassName =
  "-mb-px flex shrink-0 items-center gap-1.5 border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none [&.active]:border-primary [&.active]:text-foreground"

function TabIcon({ icon: Icon }: { icon: LucideIcon }) {
  return <Icon aria-hidden className="size-3.5" />
}

type ServerTabsProps = {
  serverId: number
}

/** Section navigation shared by every page of a server. */
function ServerTabs({ serverId }: ServerTabsProps) {
  const { defaultRange } = useMetricDefaultRange()
  const params = { serverId: String(serverId) }

  return (
    <nav
      aria-label="Server sections"
      className="-mx-1 -mb-3 flex overflow-x-auto px-1"
    >
      <Link
        to="/servers/$serverId"
        params={params}
        search={{ range: defaultRange }}
        activeOptions={{ exact: true, includeSearch: false }}
        className={tabClassName}
      >
        <TabIcon icon={LineChart} />
        Overview
      </Link>
      <Link
        to="/servers/$serverId/incidents"
        params={params}
        className={tabClassName}
      >
        <TabIcon icon={History} />
        Incidents
      </Link>
      <Link
        to="/servers/$serverId/settings"
        params={params}
        className={tabClassName}
      >
        <TabIcon icon={Settings} />
        Settings
      </Link>
    </nav>
  )
}

export { ServerTabs }
