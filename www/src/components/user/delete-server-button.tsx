import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Trash2 } from "lucide-react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { Button } from "@/components/ui/button"
import { SETTINGS_TOOLTIPS } from "@/lib/tooltips/copy"
import { deleteServer } from "@/lib/api/user/servers"
import { serverAccessQueryKey } from "@/lib/api/user/access.queries"
import { removeServerFromCaches } from "@/lib/api/user/servers.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type DeleteServerButtonProps = {
  serverId: number
  serverName: string
  onDeleted?: () => void
  variant?: "icon" | "destructive"
}

function DeleteServerButton({
  serverId,
  serverName,
  onDeleted,
  variant = "icon",
}: DeleteServerButtonProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => deleteServer(serverId),
    onSuccess: async () => {
      removeServerFromCaches(queryClient, serverId)
      queryClient.removeQueries({ queryKey: serverAccessQueryKey(serverId) })
      toastSuccess("Server deleted")
      onDeleted?.()
    },
    onError: (mutationError) => {
      toastMutationError(
        "Could not delete server",
        mutationError,
        "Failed to delete server"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        variant === "destructive" ? (
          <Button type="button" variant="destructive" size="sm">
            <Trash2 className="size-4" />
            Delete server
          </Button>
        ) : (
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            className="text-neutral-400 hover:bg-transparent hover:text-red-600 dark:text-neutral-500 dark:hover:bg-transparent dark:hover:text-red-400"
            aria-label={`Delete ${serverName}`}
          >
            <Trash2 className="size-4" />
          </Button>
        )
      }
      title="Delete server"
      triggerTooltip={SETTINGS_TOOLTIPS.deleteServer}
      description={
        <>
          Are you sure you want to delete{" "}
          <span className="font-bold">{serverName}</span>? This cannot be
          undone.
        </>
      }
      confirmLabel="Delete"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

export { DeleteServerButton }
