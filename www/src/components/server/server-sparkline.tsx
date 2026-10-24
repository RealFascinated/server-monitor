import { useQuery } from "@tanstack/react-query"

import { Sparkline } from "@/components/sparkline"
import { useInView } from "@/hooks/use-in-view"
import { userServerSparklineQueryOptions } from "@/lib/api/user/sparklines.queries"
import type { ServerResponse } from "@/lib/api/user/servers"
import { percentTrendColorClass } from "@/lib/metrics/percent-level"
import { sparklineStats } from "@/lib/metrics/sparkline"

type ServerSparklineProps = {
  server: Pick<ServerResponse, "serverId" | "status">
  metric: "cpu" | "memory"
}

const LABELS = {
  cpu: "CPU, last hour",
  memory: "Memory, last hour",
} as const

/**
 * Last-hour trend for a server row. Data is fetched only once the row has been
 * scrolled near the viewport, and only for servers that are reporting.
 */
function ServerSparkline({ server, metric }: ServerSparklineProps) {
  const { ref, seen } = useInView<HTMLSpanElement>()
  const { data } = useQuery({
    ...userServerSparklineQueryOptions(server.serverId),
    enabled: seen && server.status === "ONLINE",
  })

  const values = data?.[metric]
  const latest = values ? (sparklineStats(values)?.latest ?? null) : null

  return (
    <span ref={ref} className="inline-flex h-6 w-[72px] shrink-0 items-center">
      {values ? (
        <Sparkline
          values={values}
          label={LABELS[metric]}
          className={percentTrendColorClass(latest)}
        />
      ) : null}
    </span>
  )
}

export { ServerSparkline }
