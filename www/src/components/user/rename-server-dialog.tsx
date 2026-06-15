import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Pencil } from "lucide-react"
import { useState } from "react"

import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
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
import { renameServer } from "@/lib/api/user/servers"
import { updateServerInCaches } from "@/lib/api/user/servers.queries"
import { MAX_SERVER_NAME_LENGTH, validateServerName } from "@/lib/server-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type RenameServerDialogProps = {
  serverId: number
  currentName: string
}

function RenameServerDialog({
  serverId,
  currentName,
}: RenameServerDialogProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(currentName)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const inputId = `rename-server-name-${serverId}`

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (nextName: string) =>
      renameServer(serverId, { name: nextName }),
    onSuccess: (server) => {
      updateServerInCaches(queryClient, server)
      toastSuccess("Server renamed")
      setOpen(false)
      resetForm()
    },
    onError: (error) => {
      toastMutationError(
        "Could not rename server",
        error,
        "Failed to rename server"
      )
    },
  })

  function resetForm() {
    setName(currentName)
    setFieldError(null)
  }

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    if (nextOpen) {
      setName(currentName)
      setFieldError(null)
    } else {
      resetForm()
    }
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const error = validateServerName(name)
    if (error) {
      setFieldError(error)
      return
    }

    const trimmed = name.trim()
    if (trimmed === currentName) {
      setOpen(false)
      return
    }

    setFieldError(null)
    mutation.mutate(trimmed)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Tooltip>
        <TooltipTrigger asChild>
          <DialogTrigger asChild>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              className="text-neutral-400 hover:bg-transparent hover:text-neutral-600 dark:text-neutral-500 dark:hover:bg-transparent dark:hover:text-white"
              aria-label={`Rename ${currentName}`}
            >
              <Pencil className="size-4" />
            </Button>
          </DialogTrigger>
        </TooltipTrigger>
        <TooltipContent>Rename server</TooltipContent>
      </Tooltip>
      <DialogContent className="rounded-sm border border-neutral-200 sm:max-w-lg dark:border-monitor-gray-300">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Rename server</DialogTitle>
            <DialogDescription>
              Choose a new display name for this server. Names can be up to{" "}
              {MAX_SERVER_NAME_LENGTH} characters.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-2">
            <Label htmlFor={inputId}>Name</Label>
            <Input
              id={inputId}
              value={name}
              maxLength={MAX_SERVER_NAME_LENGTH}
              onChange={(event) => setName(event.target.value)}
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

export { RenameServerDialog }
