import { useMutation, useQueryClient } from "@tanstack/react-query"
import { LogOut } from "lucide-react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { Button } from "@/components/ui/button"
import { leaveServer } from "@/lib/api/user/access"
import { serverAccessQueryKey } from "@/lib/api/user/access.queries"
import { removeServerFromCaches } from "@/lib/api/user/servers.queries"
import { SETTINGS_TOOLTIPS } from "@/lib/tooltips/copy"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type LeaveServerButtonProps = {
  serverId: number
  serverName: string
  onLeft?: () => void
}

function LeaveServerButton({
  serverId,
  serverName,
  onLeft,
}: LeaveServerButtonProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => leaveServer(serverId),
    onSuccess: async () => {
      removeServerFromCaches(queryClient, serverId)
      queryClient.removeQueries({ queryKey: serverAccessQueryKey(serverId) })
      toastSuccess("Left server")
      onLeft?.()
    },
    onError: (mutationError) => {
      toastMutationError(
        "Could not leave server",
        mutationError,
        "Failed to leave server"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <Button type="button" variant="destructive" size="sm">
          <LogOut className="size-4" />
          Leave server
        </Button>
      }
      title="Leave server"
      triggerTooltip={SETTINGS_TOOLTIPS.leaveServer}
      description={
        <>
          Leave <span className="font-bold">{serverName}</span>? You will lose
          access to its metrics until the owner invites you again.
        </>
      }
      confirmLabel="Leave"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

export { LeaveServerButton }
