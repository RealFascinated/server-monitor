import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Trash2 } from "lucide-react"
import { useState } from "react"

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
import { deleteServerFolder } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type DeleteFolderButtonProps = {
  folderId: number
  folderName: string
}

function DeleteFolderButton({ folderId, folderName }: DeleteFolderButtonProps) {
  const [open, setOpen] = useState(false)
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => deleteServerFolder(folderId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userServersQueryKey })
      queryClient.setQueryData<ServerFolderResponse[]>(
        ["user", "server-folders"],
        (current) => current?.filter((entry) => entry.id !== folderId) ?? []
      )
      toastSuccess("Folder deleted")
      setOpen(false)
    },
    onError: (error) => {
      toastMutationError(
        "Could not delete folder",
        error,
        "Failed to delete folder"
      )
    },
  })

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <button
          type="button"
          aria-label={`Delete folder ${folderName}`}
          className="flex size-5 shrink-0 items-center justify-center rounded-sm text-neutral-400 transition-colors hover:bg-muted hover:text-error"
        >
          <Trash2 className="size-3" />
        </button>
      </DialogTrigger>
      <DialogContent className="rounded-sm border border-neutral-200 dark:border-monitor-gray-300">
        <DialogHeader>
          <DialogTitle>Delete folder</DialogTitle>
          <DialogDescription>
            Delete &ldquo;{folderName}&rdquo;? Servers in this folder will be
            ungrouped but not deleted.
          </DialogDescription>
        </DialogHeader>

        <DialogFooter className="border-t border-neutral-200 pt-3 dark:border-monitor-gray-200">
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
            variant="destructive"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? <Spinner /> : null}
            Delete
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export { DeleteFolderButton }
