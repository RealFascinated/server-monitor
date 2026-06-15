import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Pencil } from "lucide-react"
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { renameServerFolder } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { MAX_FOLDER_NAME_LENGTH, validateFolderName } from "@/lib/folder-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type RenameFolderDialogProps = {
  folderId: number
  currentName: string
}

function RenameFolderDialog({
  folderId,
  currentName,
}: RenameFolderDialogProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(currentName)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (nextName: string) =>
      renameServerFolder(folderId, { name: nextName }),
    onSuccess: (folder) => {
      void queryClient.invalidateQueries({ queryKey: userServersQueryKey })
      queryClient.setQueryData<ServerFolderResponse[]>(
        ["user", "server-folders"],
        (current) =>
          current?.map((entry) =>
            entry.id === folder.id ? folder : entry
          ) ?? [folder]
      )
      toastSuccess("Folder renamed")
      setOpen(false)
    },
    onError: (error) => {
      toastMutationError(
        "Could not rename folder",
        error,
        "Failed to rename folder"
      )
    },
  })

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    if (nextOpen) {
      setName(currentName)
      setFieldError(null)
    }
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const error = validateFolderName(name)
    if (error) {
      setFieldError(error)
      return
    }

    const trimmedName = name.trim()
    if (trimmedName === currentName) {
      setOpen(false)
      return
    }

    setFieldError(null)
    mutation.mutate(trimmedName)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <button
          type="button"
          aria-label={`Rename folder ${currentName}`}
          className="flex size-5 shrink-0 items-center justify-center rounded-sm text-neutral-400 transition-colors hover:bg-muted hover:text-foreground"
        >
          <Pencil className="size-3" />
        </button>
      </DialogTrigger>
      <DialogContent className="rounded-sm border border-neutral-200 dark:border-monitor-gray-300">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Rename folder</DialogTitle>
            <DialogDescription>
              Rename &ldquo;{currentName}&rdquo; for all servers assigned to
              this folder.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-2">
            <Label htmlFor={`rename-folder-${folderId}`}>Name</Label>
            <Input
              id={`rename-folder-${folderId}`}
              value={name}
              maxLength={MAX_FOLDER_NAME_LENGTH}
              onChange={(event) => {
                setName(event.target.value)
                setFieldError(null)
              }}
              aria-invalid={fieldError ? true : undefined}
              disabled={mutation.isPending}
              required
              autoFocus
            />
            {fieldError ? (
              <p className="text-xs font-bold text-error">{fieldError}</p>
            ) : null}
          </div>

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
              type="submit"
              variant="highlighted"
              disabled={mutation.isPending}
            >
              {mutation.isPending ? <Spinner /> : null}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export { RenameFolderDialog }
