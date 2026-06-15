import { useMutation, useQueryClient } from "@tanstack/react-query"
import { FolderPlus } from "lucide-react"
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
import { createServerFolder } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { MAX_FOLDER_NAME_LENGTH, validateFolderName } from "@/lib/folder-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

function CreateFolderDialog() {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState("")
  const [fieldError, setFieldError] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (folderName: string) =>
      createServerFolder({ name: folderName }),
    onSuccess: (folder) => {
      queryClient.setQueryData<ServerFolderResponse[]>(
        ["user", "server-folders"],
        (current) => [...(current ?? []), folder]
      )
      toastSuccess("Folder created")
      setOpen(false)
      resetForm()
    },
    onError: (error) => {
      toastMutationError(
        "Could not create folder",
        error,
        "Failed to create folder"
      )
    },
  })

  function resetForm() {
    setName("")
    setFieldError(null)
  }

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    if (!nextOpen) {
      resetForm()
    }
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const error = validateFolderName(name)
    if (error) {
      setFieldError(error)
      return
    }

    setFieldError(null)
    mutation.mutate(name.trim())
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button type="button" variant="default" size="sm">
          <FolderPlus />
          Create folder
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-sm border border-neutral-200 dark:border-monitor-gray-300">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Create folder</DialogTitle>
            <DialogDescription>
              Create a folder to organize your servers. Assign servers from
              their settings page or when editing a server.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-2">
            <Label htmlFor="folder-name">Name</Label>
            <Input
              id="folder-name"
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
              Create
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export { CreateFolderDialog }
