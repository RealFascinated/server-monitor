import { describe, expect, it } from "vitest"

import { computeAlertOverview } from "@/lib/alerts/overview"
import type { AlertGroup } from "@/lib/api/user/alerts"
import type { ServerResponse } from "@/lib/api/user/servers"

function group(
  id: number,
  serverIds: number[],
  ruleCount: number,
  firingCount = 0,
  enabled = true
): AlertGroup {
  return {
    id,
    name: `g${id}`,
    autoAddServers: false,
    enabled,
    defaultGroup: false,
    servers: serverIds.map((serverId) => ({
      id: serverId,
      name: `s${serverId}`,
    })),
    ruleCount,
    firingCount,
    createdAt: "2026-01-01T00:00:00Z",
  }
}

const servers = [1, 2, 3].map(
  (serverId) => ({ serverId, serverName: `s${serverId}` }) as ServerResponse
)

describe("computeAlertOverview", () => {
  it("totals rules and firing alerts across groups", () => {
    const overview = computeAlertOverview(
      [group(1, [1], 4, 2), group(2, [1, 2], 3, 1)],
      servers
    )

    expect(overview).toMatchObject({ firing: 3, ruleCount: 7, groupCount: 2 })
  })

  it("lists servers that are in no group", () => {
    const overview = computeAlertOverview([group(1, [1, 2], 2)], servers)

    expect(overview.uncoveredServers.map((server) => server.serverId)).toEqual([
      3,
    ])
    expect(overview.coveredServerCount).toBe(2)
  })

  it("doesn't count a group without rules as coverage", () => {
    const overview = computeAlertOverview([group(1, [1, 2], 0)], servers)

    expect(overview.coveredServerCount).toBe(0)
    expect(overview.uncoveredServers.map((server) => server.serverId)).toEqual([
      3,
    ])
  })

  it("ignores disabled groups", () => {
    const overview = computeAlertOverview(
      [group(1, [1], 4, 0, false), group(2, [2], 3, 1)],
      servers
    )

    expect(overview).toMatchObject({ firing: 1, ruleCount: 3, groupCount: 2 })
    expect(overview.coveredServerCount).toBe(1)
    expect(overview.uncoveredServers.map((server) => server.serverId)).toEqual([
      1, 3,
    ])
  })
})
