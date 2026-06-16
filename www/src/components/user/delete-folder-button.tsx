import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Trash2 } from "lucide-react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { deleteServerFolder } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServerFoldersQueryKey } from "@/lib/api/user/folders.queries"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type DeleteFolderButtonProps = {
  folderId: number
  folderName: string
}

function DeleteFolderButton({ folderId, folderName }: DeleteFolderButtonProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => deleteServerFolder(folderId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userServersQueryKey })
      queryClient.setQueryData<ServerFolderResponse[]>(
        userServerFoldersQueryKey,
        (current) => current?.filter((entry) => entry.id !== folderId) ?? []
      )
      toastSuccess("Folder deleted")
    },
    onError: (error) => {
      toastMutationError(
        "Could not delete folder",
        error,
        "Failed to delete folder"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <button
          type="button"
          aria-label={`Delete folder ${folderName}`}
          className="flex size-5 shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-muted hover:text-error"
        >
          <Trash2 className="size-3" />
        </button>
      }
      title="Delete folder"
      description={
        <>
          Delete &ldquo;{folderName}&rdquo;? Servers in this folder will be
          ungrouped but not deleted.
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

export { DeleteFolderButton }
