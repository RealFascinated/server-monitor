import { describe, expect, it } from "vitest"

import type { ServerResponse, ServerStatus } from "@/lib/api/user/servers"
import {
  countByFleetFilter,
  serverMatchesFilter,
  sortServerIds,
} from "@/lib/servers/fleet-filter"

function makeServer(
  serverId: number,
  status: ServerStatus,
  overrides: Partial<ServerResponse> = {}
): ServerResponse {
  return {
    serverId,
    serverName: `server-${serverId}`,
    status,
    uptimeSeconds: null,
    uptimePercent30d: null,
    agentVersion: null,
    createdAt: "2026-01-01T00:00:00Z",
    role: "OWNER",
    permissions: 0,
    folderName: null,
    inventory: null,
    cpu: null,
    memory: null,
    disk: null,
    ...overrides,
  }
}

const cpu = (percent: number) => ({
  percent,
  user: null,
  system: null,
  iowait: null,
  steal: null,
})

const healthy = makeServer(1, "ONLINE", { cpu: cpu(10) })
const hot = makeServer(2, "ONLINE", { cpu: cpu(97) })
const warm = makeServer(3, "ONLINE", { cpu: cpu(85) })
const down = makeServer(4, "OFFLINE")
const fresh = makeServer(5, "PENDING")
const all = [healthy, hot, warm, down, fresh]
const byId = Object.fromEntries(all.map((server) => [server.serverId, server]))

describe("serverMatchesFilter", () => {
  it("matches by status", () => {
    expect(serverMatchesFilter(down, "offline")).toBe(true)
    expect(serverMatchesFilter(healthy, "offline")).toBe(false)
    expect(serverMatchesFilter(fresh, "pending")).toBe(true)
    expect(serverMatchesFilter(healthy, "online")).toBe(true)
  })

  it("treats offline and high-usage servers as needing attention", () => {
    expect(serverMatchesFilter(down, "attention")).toBe(true)
    expect(serverMatchesFilter(hot, "attention")).toBe(true)
    expect(serverMatchesFilter(healthy, "attention")).toBe(false)
    expect(serverMatchesFilter(fresh, "attention")).toBe(false)
  })
})

describe("countByFleetFilter", () => {
  it("counts every bucket", () => {
    expect(countByFleetFilter(all)).toEqual({
      all: 5,
      online: 3,
      offline: 1,
      pending: 1,
      attention: 3,
    })
  })
})

describe("sortServerIds", () => {
  const ids = [1, 2, 3, 4, 5]

  it("keeps the given order for custom sort", () => {
    expect(sortServerIds(ids, byId, "custom")).toEqual(ids)
  })

  it("puts offline first, then the most loaded servers, then healthy", () => {
    expect(sortServerIds(ids, byId, "attention")).toEqual([4, 2, 3, 1, 5])
  })

  it("is stable for equal scores", () => {
    const a = makeServer(10, "ONLINE", { cpu: cpu(5) })
    const b = makeServer(11, "ONLINE", { cpu: cpu(5) })
    const map = { 10: a, 11: b }
    expect(sortServerIds([11, 10], map, "attention")).toEqual([11, 10])
  })

  it("sorts by name naturally", () => {
    const x = makeServer(20, "ONLINE", { serverName: "node-10" })
    const y = makeServer(21, "ONLINE", { serverName: "node-2" })
    const map = { 20: x, 21: y }
    expect(sortServerIds([20, 21], map, "name")).toEqual([21, 20])
  })

  it("sorts by cpu descending with unknown values last", () => {
    expect(sortServerIds(ids, byId, "cpu")).toEqual([2, 3, 1, 4, 5])
  })

  it("does not mutate its input", () => {
    const input = [...ids]
    sortServerIds(input, byId, "attention")
    expect(input).toEqual(ids)
  })
})
