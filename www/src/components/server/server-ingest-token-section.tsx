import { useMutation } from "@tanstack/react-query"
import { AlertTriangle, Terminal } from "lucide-react"
import { useState } from "react"

import { AgentInstallPanel } from "@/components/server/agent-install-panel"
import { ServerStatusBadge } from "@/components/server/server-status-badge"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { rotateIngestToken } from "@/lib/api/user/servers"
import type { ServerStatus } from "@/lib/api/user/servers"
import {
  SERVER_TABLE_COLUMN_TOOLTIPS,
  SETTINGS_TOOLTIPS,
} from "@/lib/tooltips/copy"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type ServerIngestTokenSectionProps = {
  serverId: number
  status: ServerStatus
  agentVersion: string | null
}

function ServerIngestTokenSection({
  serverId,
  status,
  agentVersion,
}: ServerIngestTokenSectionProps) {
  const [ingestToken, setIngestToken] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => rotateIngestToken(serverId),
    onSuccess: (response) => {
      setIngestToken(response.ingestToken)
      toastSuccess("Token rotated")
    },
    onError: (error) => {
      toastMutationError(
        "Could not rotate ingest token",
        error,
        "Failed to rotate ingest token"
      )
    },
  })

  function handleRotate() {
    mutation.mutate()
  }

  return (
    <div className="overflow-hidden rounded-sm border border-neutral-200 dark:border-monitor-gray-300">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-neutral-200 bg-neutral-50/80 px-4 py-2.5 dark:border-monitor-gray-300 dark:bg-monitor-gray-100/40">
        <ServerStatusBadge status={status} />
        <SimpleTooltip content={SERVER_TABLE_COLUMN_TOOLTIPS.agent}>
          <span className="cursor-help text-xs text-neutral-500">
            {agentVersion ? (
              <>
                Agent{" "}
                <span className="font-medium text-foreground">
                  {agentVersion}
                </span>
              </>
            ) : (
              "No agent version reported"
            )}
          </span>
        </SimpleTooltip>
      </div>

      <div className="flex flex-col gap-4 p-4">
        {ingestToken ? (
          <AgentInstallPanel ingestToken={ingestToken} />
        ) : (
          <>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex min-w-0 flex-col gap-1">
                <p className="text-sm font-medium">Ingest token</p>
                <p className="text-xs font-bold text-neutral-500">
                  Authenticates the Monitor Agent on this host. Rotate when
                  setting up a new installation or replacing a lost token.
                </p>
              </div>

              <SimpleTooltip content={SETTINGS_TOOLTIPS.rotateIngestToken}>
                <Button
                  type="button"
                  variant="highlighted"
                  size="sm"
                  className="shrink-0 cursor-help"
                  disabled={mutation.isPending}
                  onClick={handleRotate}
                >
                  {mutation.isPending ? (
                    <Spinner />
                  ) : (
                    <Terminal className="size-4" />
                  )}
                  Rotate token
                </Button>
              </SimpleTooltip>
            </div>

            <p className="flex items-start gap-2 text-xs text-warning-700 dark:text-warning-300">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0" aria-hidden />
              <span>
                Rotating revokes the current token. Update the agent
                configuration on your host with the new one.
              </span>
            </p>
          </>
        )}
      </div>
    </div>
  )
}

export { ServerIngestTokenSection }
