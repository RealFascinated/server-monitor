import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { Plus } from "lucide-react"
import { useState } from "react"

import { AgentInstallPanel } from "@/components/server/agent-install-panel"
import { WaitingForServerData } from "@/components/server/waiting-for-server-data"
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
  Select,
  SelectContent,
  SelectItem,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { userServerFoldersQueryOptions } from "@/lib/api/user/folders.queries"
import { createServer } from "@/lib/api/user/servers"
import type { CreatedServerResponse } from "@/lib/api/user/servers"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import {
  MAX_FOLDER_NAME_LENGTH,
  validateFolderName,
} from "@/lib/folder-name"
import { invalidateServerFoldersIfNeeded } from "@/lib/servers/folder-queries"
import { MAX_SERVER_NAME_LENGTH, validateServerName } from "@/lib/server-name"
import { toastMutationError } from "@/lib/toast"

const NO_FOLDER_VALUE = "__none__"
const NEW_FOLDER_VALUE = "__new__"

type CreateServerDialogProps = {
  defaultFolderName?: string
  trigger?: React.ReactNode
}

function CreateServerDialog({
  defaultFolderName,
  trigger,
}: CreateServerDialogProps = {}) {
  const initialFolderChoice = defaultFolderName ?? NO_FOLDER_VALUE
  const [open, setOpen] = useState(false)
  const [name, setName] = useState("")
  const [folderChoice, setFolderChoice] = useState(initialFolderChoice)
  const [newFolderName, setNewFolderName] = useState("")
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [folderError, setFolderError] = useState<string | null>(null)
  const [createdServer, setCreatedServer] =
    useState<CreatedServerResponse | null>(null)

  const queryClient = useQueryClient()
  const { data: folders = [] } = useQuery({
    ...userServerFoldersQueryOptions(),
    enabled: open,
  })

  const mutation = useMutation({
    mutationFn: createServer,
    onSuccess: async (server, variables) => {
      await queryClient.invalidateQueries({ queryKey: userServersQueryKey })
      invalidateServerFoldersIfNeeded(queryClient, variables.folderName ?? null)
      setCreatedServer(server)
    },
    onError: (error) => {
      toastMutationError(
        "Could not create server",
        error,
        "Failed to create server"
      )
    },
  })

  function resetForm() {
    setName("")
    setFolderChoice(initialFolderChoice)
    setNewFolderName("")
    setFieldError(null)
    setFolderError(null)
    setCreatedServer(null)
  }

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    if (nextOpen) {
      setName("")
      setFolderChoice(initialFolderChoice)
      setNewFolderName("")
      setFieldError(null)
      setFolderError(null)
      setCreatedServer(null)
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

    const trimmedFolder =
      folderChoice === NEW_FOLDER_VALUE
        ? newFolderName.trim()
        : folderChoice === NO_FOLDER_VALUE
          ? ""
          : folderChoice
    const folderValidationError =
      trimmedFolder === ""
        ? null
        : validateFolderName(
            folderChoice === NEW_FOLDER_VALUE ? newFolderName : trimmedFolder
          )
    if (folderValidationError) {
      setFolderError(folderValidationError)
      return
    }

    setFieldError(null)
    setFolderError(null)
    mutation.mutate({
      name: name.trim(),
      folderName: trimmedFolder || null,
    })
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button type="button" variant="highlighted" size="sm">
            <Plus />
            Create server
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="rounded-sm border border-neutral-200 sm:max-w-2xl dark:border-monitor-gray-300">
        {createdServer ? (
          <>
            <DialogHeader>
              <DialogTitle>Install the Monitor Agent</DialogTitle>
              <DialogDescription>
                <span className="font-medium text-foreground">
                  {createdServer.serverName}
                </span>{" "}
                was created. Install the agent on your host using the token and
                command below.
              </DialogDescription>
            </DialogHeader>

            <WaitingForServerData serverId={createdServer.serverId} />

            <AgentInstallPanel ingestToken={createdServer.ingestToken} />

            <DialogFooter className="border-t border-neutral-200 pt-3 dark:border-monitor-gray-200">
              <Button
                type="button"
                variant="default"
                onClick={() => handleOpenChange(false)}
              >
                Done
              </Button>
              <Button type="button" variant="highlighted" asChild>
                <Link
                  to="/servers/$serverId"
                  params={{ serverId: String(createdServer.serverId) }}
                  search={{ range: "1h" }}
                  onClick={() => handleOpenChange(false)}
                >
                  View server
                </Link>
              </Button>
            </DialogFooter>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle>Create server</DialogTitle>
              <DialogDescription>
                Register a new server to monitor. After creation you will
                receive an ingest token and install instructions for the Monitor
                Agent.
              </DialogDescription>
            </DialogHeader>

            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="server-name">Name</Label>
                <Input
                  id="server-name"
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

              <div className="flex flex-col gap-2">
                <Label htmlFor="server-folder">Folder</Label>
                <Select
                  value={folderChoice}
                  onValueChange={(value) => {
                    setFolderChoice(value)
                    setFolderError(null)
                  }}
                  disabled={mutation.isPending}
                >
                  <SelectTrigger
                    id="server-folder"
                    className="w-full"
                    aria-invalid={folderError ? true : undefined}
                  >
                    <SelectValue placeholder="No folder" />
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    className="w-(--radix-select-trigger-width)"
                  >
                    <SelectItem value={NO_FOLDER_VALUE}>No folder</SelectItem>
                    {folders.length > 0 ? (
                      <>
                        <SelectSeparator />
                        {folders.map((folder) => (
                          <SelectItem key={folder.id} value={folder.name}>
                            {folder.name}
                          </SelectItem>
                        ))}
                      </>
                    ) : null}
                    <SelectSeparator />
                    <SelectItem value={NEW_FOLDER_VALUE}>
                      New folder…
                    </SelectItem>
                  </SelectContent>
                </Select>
                {folderChoice === NEW_FOLDER_VALUE ? (
                  <Input
                    value={newFolderName}
                    maxLength={MAX_FOLDER_NAME_LENGTH}
                    onChange={(event) => {
                      setNewFolderName(event.target.value)
                      setFolderError(null)
                    }}
                    placeholder="Folder name"
                    aria-invalid={folderError ? true : undefined}
                    disabled={mutation.isPending}
                    autoFocus
                  />
                ) : null}
                {folderError ? (
                  <p className="text-xs font-bold text-error">{folderError}</p>
                ) : null}
              </div>
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
        )}
      </DialogContent>
    </Dialog>
  )
}

export { CreateServerDialog }
