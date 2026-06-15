import { Box, Clock, Cpu, Globe, HardDrive, MemoryStick } from "lucide-react"
import type { LucideIcon } from "lucide-react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerInventory, ServerResponse } from "@/lib/api/user/servers"
import {
  formatMemoryBytes,
  formatMemoryUsage,
  formatUptime,
  formatUptimeDetailed,
} from "@/lib/formatter"
import { cn } from "@/lib/utils"

type ServerMetaSubtitleProps = {
  server: ServerResponse
  prefix?: string
  className?: string
}

type MetaItem = {
  key: string
  icon: LucideIcon
  label: string
  value: string
  tooltip?: string
}

function formatOs(inventory: ServerInventory): string | null {
  const parts = [inventory.osName, inventory.osVersion].filter(Boolean)
  return parts.length > 0 ? parts.join(" ") : null
}

function pushMetaItem(
  items: MetaItem[],
  value: string | null | undefined,
  meta: Omit<MetaItem, "value">
) {
  if (value) {
    items.push({ ...meta, value })
  }
}

function buildMetaItems(server: ServerResponse): MetaItem[] {
  const inventory = server.inventory
  const items: MetaItem[] = []

  pushMetaItem(items, inventory?.ip, {
    key: "ip",
    icon: Globe,
    label: "IP address",
    tooltip: "As reported by the Monitor Agent.",
  })

  const uptimeFormatted =
    server.uptimeSeconds != null ? formatUptime(server.uptimeSeconds) : null
  const uptimeDetailed = formatUptimeDetailed(server.uptimeSeconds)
  pushMetaItem(items, uptimeFormatted, {
    key: "uptime",
    icon: Clock,
    label: "Uptime",
    tooltip:
      uptimeDetailed && uptimeDetailed !== uptimeFormatted
        ? uptimeDetailed
        : undefined,
  })

  pushMetaItem(items, inventory ? formatOs(inventory) : null, {
    key: "os",
    icon: Box,
    label: "Operating system",
  })

  return items
}

function MetaChip({ item }: { item: MetaItem }) {
  const Icon = item.icon
  const chip = (
    <span className="inline-flex items-center gap-1.5">
      <Icon className="size-3.5 shrink-0 opacity-70" aria-hidden />
      <span aria-label={item.label}>{item.value}</span>
    </span>
  )

  if (!item.tooltip) {
    return chip
  }

  return (
    <SimpleTooltip content={item.tooltip}>
      <span className="cursor-help">{chip}</span>
    </SimpleTooltip>
  )
}

function ServerMetaSubtitle({
  server,
  prefix,
  className,
}: ServerMetaSubtitleProps) {
  const items = buildMetaItems(server)

  if (items.length === 0 && !prefix) {
    return null
  }

  return (
    <div
      className={cn(
        "flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground",
        className
      )}
    >
      {prefix ? <span>{prefix}</span> : null}
      {prefix && items.length > 0 ? (
        <span className="text-muted-foreground/40" aria-hidden>
          |
        </span>
      ) : null}
      {items.map((item, index) => (
        <span key={item.key} className="inline-flex items-center gap-2">
          {index > 0 ? (
            <span className="text-muted-foreground/40" aria-hidden>
              |
            </span>
          ) : null}
          <MetaChip item={item} />
        </span>
      ))}
    </div>
  )
}

export { ServerMetaSubtitle }
