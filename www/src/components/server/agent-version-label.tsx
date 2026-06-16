import { useQuery } from "@tanstack/react-query"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { latestAgentVersionQueryOptions } from "@/lib/api/agent.queries"
import { isVersionOlderThan } from "@/lib/agent/semver"
import { cn } from "@/lib/utils"

type AgentVersionLabelProps = {
  version: string | null
  layout?: "table" | "settings"
}

function AgentUpdateTooltip({ latestVersion }: { latestVersion: string }) {
  return (
    <span>
      Version <span className="font-medium">{latestVersion}</span> is available.
      Run{" "}
      <code className="rounded-sm bg-neutral-300/70 px-1 py-0.5 font-mono font-medium text-foreground dark:bg-monitor-gray-500/70">
        monitor-agent update
      </code>{" "}
      on the host to upgrade.
    </span>
  )
}

function AgentVersionLabel({
  version,
  layout = "table",
}: AgentVersionLabelProps) {
  const { data: latest } = useQuery(latestAgentVersionQueryOptions())
  const availableUpdateVersion =
    version != null &&
    latest != null &&
    isVersionOlderThan(version, latest.version)
      ? latest.version
      : null

  if (!version) {
    return layout === "settings" ? (
      <span className="text-xs text-neutral-500">
        No agent version reported
      </span>
    ) : (
      <span>—</span>
    )
  }

  const versionNode = (
    <span
      className={cn(layout === "settings" && "font-medium text-foreground")}
    >
      {version}
    </span>
  )

  return (
    <span className="inline-flex flex-wrap items-center gap-1.5">
      {layout === "settings" ? <>Agent {versionNode}</> : versionNode}
      {availableUpdateVersion ? (
        <SimpleTooltip
          content={
            <AgentUpdateTooltip latestVersion={availableUpdateVersion} />
          }
        >
          <span
            className={cn(
              "inline-flex cursor-help items-center rounded-sm bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
              layout === "settings" && "text-xs"
            )}
          >
            {layout === "table" ? "Update" : "Update available"}
          </span>
        </SimpleTooltip>
      ) : null}
    </span>
  )
}

export { AgentVersionLabel }
