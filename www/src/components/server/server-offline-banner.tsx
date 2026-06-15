import { Link } from "@tanstack/react-router"
import { useQuery } from "@tanstack/react-query"
import { useEffect, useState } from "react"

import { Callout } from "@/components/callout"
import { serverOpenIncidentQueryOptions } from "@/lib/api/user/incidents.queries"
import type { ServerStatus } from "@/lib/api/user/servers"
import { userServerStatusQueryOptions } from "@/lib/api/user/servers.queries"
import { formatDurationSeconds } from "@/lib/formatter"

type ServerOfflineBannerProps = {
  serverId: number
  status: ServerStatus
}

function ServerOfflineBanner({ serverId, status }: ServerOfflineBannerProps) {
  const { data: openIncident } = useQuery({
    ...serverOpenIncidentQueryOptions(serverId),
    enabled: status === "OFFLINE",
  })
  const { data: serverStatus } = useQuery({
    ...userServerStatusQueryOptions(serverId),
    enabled: status === "OFFLINE",
  })

  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (status !== "OFFLINE") {
      return
    }

    const interval = window.setInterval(() => {
      setNow(Date.now())
    }, 30_000)

    return () => {
      window.clearInterval(interval)
    }
  }, [status])

  if (status !== "OFFLINE") {
    return null
  }

  const outageStartedAt =
    openIncident?.startedAt ?? serverStatus?.lastHeartbeat ?? null

  const durationLabel = outageStartedAt
    ? formatDurationSeconds(
        Math.max(
          0,
          Math.floor(
            (now - new Date(outageStartedAt).getTime()) / 1000
          )
        )
      )
    : null

  return (
    <Callout type="danger" title="Server offline">
      <div className="flex flex-col gap-2">
        <p>
          The agent has not reported recently.
          {durationLabel ? (
            <>
              {" "}
              Offline for <span className="font-medium">{durationLabel}</span>.
            </>
          ) : null}
        </p>
        <p>
          <Link
            to="/servers/$serverId/incidents"
            params={{ serverId: String(serverId) }}
            className="font-medium underline underline-offset-2"
          >
            View incident history
          </Link>
        </p>
      </div>
    </Callout>
  )
}

export { ServerOfflineBanner }
