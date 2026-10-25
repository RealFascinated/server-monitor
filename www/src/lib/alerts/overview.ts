import type { AlertGroup } from "@/lib/api/user/alerts"
import type { ServerResponse } from "@/lib/api/user/servers"

export type AlertOverview = {
  firing: number
  ruleCount: number
  groupCount: number
  coveredServerCount: number
  /** Servers in no enabled group, so no alert checks them. */
  uncoveredServers: ServerResponse[]
}

export function computeAlertOverview(
  groups: AlertGroup[],
  servers: ServerResponse[]
): AlertOverview {
  // A disabled group evaluates nothing, so it neither covers servers nor counts its alerts.
  const active = groups.filter((group) => group.enabled)
  const covered = new Set(
    active.flatMap((group) => group.servers.map((server) => server.id))
  )
  // A group with no rules checks nothing, so it doesn't count as protection.
  const protectedServers = new Set(
    active
      .filter((group) => group.ruleCount > 0)
      .flatMap((group) => group.servers.map((server) => server.id))
  )

  return {
    firing: active.reduce((sum, group) => sum + group.firingCount, 0),
    ruleCount: active.reduce((sum, group) => sum + group.ruleCount, 0),
    groupCount: groups.length,
    coveredServerCount: protectedServers.size,
    uncoveredServers: servers.filter((server) => !covered.has(server.serverId)),
  }
}
