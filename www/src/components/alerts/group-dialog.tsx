import { Plus } from "lucide-react"
import { useState } from "react"

import { FormFieldError } from "@/components/form-field-error"
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
import { SettingsToggle } from "@/components/ui/settings-toggle"
import {
  useCreateAlertGroup,
  useUpdateAlertGroup,
} from "@/hooks/use-alert-groups"
import { useUserServers } from "@/hooks/use-user-servers"
import type { AlertGroup } from "@/lib/api/user/alerts"

type GroupDialogProps = {
  /** Editing when set, creating otherwise. */
  group?: AlertGroup
  trigger?: React.ReactNode
  onCreated?: (group: AlertGroup) => void
}

function validateName(name: string): string | null {
  if (name.trim().length === 0) return "Enter a name"
  if (name.trim().length > 64) return "Name must be at most 64 characters"
  return null
}

function GroupDialog({ group, trigger, onCreated }: GroupDialogProps) {
  const editing = group != null
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(group?.name ?? "")
  const [autoAdd, setAutoAdd] = useState(group?.autoAddServers ?? false)
  const [serverIds, setServerIds] = useState<Set<number>>(
    () => new Set(group?.servers.map((server) => server.id))
  )
  const [nameError, setNameError] = useState<string | null>(null)

  const { data: servers } = useUserServers()
  const create = useCreateAlertGroup()
  const update = useUpdateAlertGroup()
  const pending = create.isPending || update.isPending

  function reset() {
    setName(group?.name ?? "")
    setAutoAdd(group?.autoAddServers ?? false)
    setServerIds(new Set(group?.servers.map((server) => server.id)))
    setNameError(null)
  }

  function handleOpenChange(nextOpen: boolean) {
    if (pending) return
    setOpen(nextOpen)
    if (nextOpen) reset()
  }

  function toggleServer(id: number, checked: boolean) {
    setServerIds((current) => {
      const next = new Set(current)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const error = validateName(name)
    setNameError(error)
    if (error) return

    const request = {
      name: name.trim(),
      autoAddServers: autoAdd,
      serverIds: [...serverIds],
    }

    if (group) {
      update.mutate(
        { id: group.id, request },
        { onSuccess: () => setOpen(false) }
      )
      return
    }

    create.mutate(request, {
      onSuccess: (created) => {
        setOpen(false)
        onCreated?.(created)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button type="button" variant="highlighted" size="sm">
            <Plus />
            New group
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>
              {editing ? "Edit group" : "New alert group"}
            </DialogTitle>
            <DialogDescription>
              Every server in a group inherits all of the group&apos;s alerts. A
              server can be in several groups.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="group-name">Name</Label>
              <Input
                id="group-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                aria-invalid={nameError ? true : undefined}
                disabled={pending}
                autoFocus
                placeholder="Production"
              />
              <FormFieldError error={nameError ?? undefined} />
            </div>

            <div className="flex items-center justify-between gap-4 rounded-md border border-border p-3">
              <div className="flex flex-col gap-0.5">
                <Label htmlFor="group-auto-add">
                  Add new servers automatically
                </Label>
                <p className="text-xs text-muted-foreground">
                  Servers you create or are invited to join this group.
                </p>
              </div>
              <SettingsToggle
                ariaLabel="Add new servers automatically"
                checked={autoAdd}
                disabled={pending}
                onCheckedChange={setAutoAdd}
              />
            </div>

            <fieldset className="flex flex-col gap-2">
              <legend className="mb-1 text-sm font-medium text-foreground">
                Servers
              </legend>
              {servers && servers.length > 0 ? (
                <ul className="flex max-h-56 flex-col overflow-y-auto rounded-md border border-border">
                  {servers.map((server) => (
                    <li
                      key={server.serverId}
                      className="border-b border-border last:border-b-0"
                    >
                      <label className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm hover:bg-muted/50">
                        <input
                          type="checkbox"
                          className="size-4 accent-primary"
                          checked={serverIds.has(server.serverId)}
                          disabled={pending}
                          onChange={(event) =>
                            toggleServer(server.serverId, event.target.checked)
                          }
                        />
                        <span className="truncate">{server.serverName}</span>
                      </label>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">
                  You have no servers yet.
                </p>
              )}
            </fieldset>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="default"
              disabled={pending}
              onClick={() => handleOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="highlighted" disabled={pending}>
              {pending ? <Spinner /> : null}
              {editing ? "Save" : "Create group"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export { GroupDialog }
