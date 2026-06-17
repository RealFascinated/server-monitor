import { Terminal } from "lucide-react"
import { useState } from "react"

import { Callout } from "@/components/callout"
import { AgentSetupPanel } from "@/components/server/agent-setup-panel"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { useRotateIngestToken } from "@/hooks/use-rotate-ingest-token"

type ServerAgentSetupDialogProps = {
  serverId: number
  serverName: string
}

function ServerAgentSetupDialog({
  serverId,
  serverName,
}: ServerAgentSetupDialogProps) {
  const [open, setOpen] = useState(false)
  const { ingestToken, mutation, resetToken } = useRotateIngestToken({
    serverId,
    errorTitle: "Could not issue ingest token",
    errorFallback: "Failed to issue ingest token",
  })

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    if (!nextOpen) {
      resetToken()
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button type="button" variant="highlighted" size="sm">
          <Terminal className="size-4" />
          Install agent
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-sm border border-border sm:max-w-2xl">
        {ingestToken ? (
          <>
            <DialogHeader>
              <DialogTitle>Install the Monitor Agent</DialogTitle>
              <DialogDescription>
                Configure the agent on{" "}
                <span className="font-medium text-foreground">
                  {serverName}
                </span>{" "}
                with the new ingest token below.
              </DialogDescription>
            </DialogHeader>

            <AgentSetupPanel serverId={serverId} ingestToken={ingestToken} />

            <DialogFooter>
              <Button
                type="button"
                variant="highlighted"
                onClick={() => handleOpenChange(false)}
              >
                Done
              </Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Install the Monitor Agent</DialogTitle>
              <DialogDescription>
                {serverName} is waiting for its first metrics report. Issue an
                ingest token to configure the agent on your host.
              </DialogDescription>
            </DialogHeader>

            <Callout type="warning" title="This invalidates any previous token">
              Generating a new ingest token revokes the previous one. Only do
              this if you have not configured the agent yet, or you need to
              replace a lost token.
            </Callout>

            <DialogFooter>
              <Button
                type="button"
                variant="default"
                disabled={mutation.isPending}
                onClick={() => handleOpenChange(false)}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="highlighted"
                disabled={mutation.isPending}
                onClick={() => mutation.mutate()}
              >
                {mutation.isPending ? <Spinner /> : null}
                Issue ingest token
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

export { ServerAgentSetupDialog }
