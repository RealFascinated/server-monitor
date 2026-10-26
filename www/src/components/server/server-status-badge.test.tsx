// @vitest-environment jsdom
import type { ReactNode } from "react"
import { cleanup, render, screen } from "@testing-library/react"
import { afterEach, describe, expect, it } from "vitest"

import { ServerStatusBadge } from "@/components/server/server-status-badge"
import { TooltipProvider } from "@/components/ui/tooltip"
import {
  formatRelativeTime,
  formatTimestampWithRelative,
} from "@/lib/formatter"

function renderBadge(node: ReactNode) {
  return render(<TooltipProvider>{node}</TooltipProvider>)
}

const hoursAgo = (hours: number) =>
  new Date(Date.now() - hours * 3_600_000).toISOString()

afterEach(cleanup)

describe("ServerStatusBadge last heartbeat", () => {
  it("shows the relative time next to an offline badge", () => {
    renderBadge(
      <ServerStatusBadge status="OFFLINE" lastHeartbeat={hoursAgo(3)} />
    )

    expect(screen.getByText("OFFLINE")).toBeTruthy()
    expect(screen.getByText("3 hours ago")).toBeTruthy()
  })

  it("shows only the badge when no heartbeat is known", () => {
    const { container } = renderBadge(
      <ServerStatusBadge status="OFFLINE" lastHeartbeat={null} />
    )

    expect(container.textContent).toBe("OFFLINE")
  })

  it("leaves online and pending badges unchanged", () => {
    const online = renderBadge(
      <ServerStatusBadge status="ONLINE" lastHeartbeat={hoursAgo(1)} />
    )
    expect(online.container.textContent).toBe("ONLINE")

    const pending = renderBadge(<ServerStatusBadge status="PENDING" />)
    expect(pending.container.textContent).toBe("PENDING")
  })

  it("pairs the absolute time with the relative time for tooltips", () => {
    const iso = hoursAgo(27)

    expect(formatTimestampWithRelative(iso)).toContain(
      `(${formatRelativeTime(iso)})`
    )
  })
})
