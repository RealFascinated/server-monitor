import { useMutation, useQueryClient } from "@tanstack/react-query"
import { FolderPlus, Pencil } from "lucide-react"
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
import {
  createServerFolder,
  renameServerFolder,
} from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServerFoldersQueryKey } from "@/lib/api/user/folders.queries"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { MAX_FOLDER_NAME_LENGTH, validateFolderName } from "@/lib/folder-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type FolderNameDialogProps = {
  trigger: React.ReactNode
  title: string
  description: React.ReactNode
  inputId: string
  initialName: string
  submitLabel: string
  isPending: boolean
  unchangedName?: string
  onSubmit: (name: string) => void | Promise<void>
}

function FolderNameDialog({
  trigger,
  title,
  description,
  inputId,
  initialName,
  submitLabel,
  isPending,
  unchangedName,
  onSubmit,
}: FolderNameDialogProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(initialName)
  const [fieldError, setFieldError] = useState<string | null>(null)

  function handleOpenChange(nextOpen: boolean) {
    if (isPending) {
      return
    }

    setOpen(nextOpen)
    setName(initialName)
    setFieldError(null)
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const error = validateFolderName(name)
    if (error) {
      setFieldError(error)
      return
    }

    const trimmedName = name.trim()
    if (unchangedName !== undefined && trimmedName === unchangedName) {
      setOpen(false)
      return
    }

    setFieldError(null)

    try {
      await onSubmit(trimmedName)
      setOpen(false)
    } catch {
      // Caller surfaces errors via toast.
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className="rounded-sm border border-border">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-2">
            <Label htmlFor={inputId}>Name</Label>
            <Input
              id={inputId}
              value={name}
              maxLength={MAX_FOLDER_NAME_LENGTH}
              onChange={(event) => {
                setName(event.target.value)
                setFieldError(null)
              }}
              aria-invalid={fieldError ? true : undefined}
              disabled={isPending}
              required
              autoFocus
            />
            {fieldError ? (
              <p className="text-xs font-bold text-error">{fieldError}</p>
            ) : null}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="default"
              disabled={isPending}
              onClick={() => handleOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="highlighted" disabled={isPending}>
              {isPending ? <Spinner /> : null}
              {submitLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function CreateFolderDialog() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (folderName: string) =>
      createServerFolder({ name: folderName }),
    onSuccess: (folder) => {
      queryClient.setQueryData<ServerFolderResponse[]>(
        userServerFoldersQueryKey,
        (current) => [...(current ?? []), folder]
      )
      toastSuccess("Folder created")
    },
    onError: (error) => {
      toastMutationError(
        "Could not create folder",
        error,
        "Failed to create folder"
      )
    },
  })

  return (
    <FolderNameDialog
      trigger={
        <Button type="button" variant="default" size="sm">
          <FolderPlus />
          Create folder
        </Button>
      }
      title="Create folder"
      description="Create a folder to organize your servers. Assign servers from their settings page or when editing a server."
      inputId="folder-name"
      initialName=""
      submitLabel="Create"
      isPending={mutation.isPending}
      onSubmit={(folderName) => mutation.mutateAsync(folderName)}
    />
  )
}

type RenameFolderDialogProps = {
  folderId: number
  currentName: string
}

function RenameFolderDialog({
  folderId,
  currentName,
}: RenameFolderDialogProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (nextName: string) =>
      renameServerFolder(folderId, { name: nextName }),
    onSuccess: (folder) => {
      void queryClient.invalidateQueries({ queryKey: userServersQueryKey })
      queryClient.setQueryData<ServerFolderResponse[]>(
        userServerFoldersQueryKey,
        (current) =>
          current?.map((entry) =>
            entry.id === folder.id ? folder : entry
          ) ?? [folder]
      )
      toastSuccess("Folder renamed")
    },
    onError: (error) => {
      toastMutationError(
        "Could not rename folder",
        error,
        "Failed to rename folder"
      )
    },
  })

  return (
    <FolderNameDialog
      trigger={
        <button
          type="button"
          aria-label={`Rename folder ${currentName}`}
          className="flex size-5 shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <Pencil className="size-3" />
        </button>
      }
      title="Rename folder"
      description={
        <>
          Rename &ldquo;{currentName}&rdquo; for all servers assigned to this
          folder.
        </>
      }
      inputId={`rename-folder-${folderId}`}
      initialName={currentName}
      unchangedName={currentName}
      submitLabel="Save"
      isPending={mutation.isPending}
      onSubmit={(nextName) => mutation.mutateAsync(nextName)}
    />
  )
}

export { CreateFolderDialog, RenameFolderDialog }
