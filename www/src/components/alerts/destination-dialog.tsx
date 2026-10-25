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
import {
  useCreateAlertDestination,
  useUpdateAlertDestination,
} from "@/hooks/use-alert-destinations"
import type { AlertDestination } from "@/lib/api/user/alerts"

type DestinationDialogProps = {
  /** Editing when set, creating otherwise. */
  destination?: AlertDestination
  trigger?: React.ReactNode
  onCreated?: (destination: AlertDestination) => void
}

function validateName(name: string): string | null {
  if (name.trim().length === 0) return "Enter a name"
  if (name.trim().length > 64) return "Name must be at most 64 characters"
  return null
}

function validateWebhookUrl(url: string, required: boolean): string | null {
  const trimmed = url.trim()
  if (trimmed.length === 0) return required ? "Enter the webhook URL" : null
  if (!/^https?:\/\//i.test(trimmed)) return "Enter a full webhook URL"
  return null
}

function DestinationDialog({
  destination,
  trigger,
  onCreated,
}: DestinationDialogProps) {
  const editing = destination != null
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(destination?.name ?? "")
  const [url, setUrl] = useState("")
  const [errors, setErrors] = useState<{ name?: string; url?: string }>({})

  const create = useCreateAlertDestination()
  const update = useUpdateAlertDestination()
  const pending = create.isPending || update.isPending

  function reset() {
    setName(destination?.name ?? "")
    setUrl("")
    setErrors({})
  }

  function handleOpenChange(nextOpen: boolean) {
    if (pending) return
    setOpen(nextOpen)
    if (nextOpen) reset()
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors = {
      name: validateName(name) ?? undefined,
      url: validateWebhookUrl(url, !editing) ?? undefined,
    }
    setErrors(nextErrors)
    if (nextErrors.name || nextErrors.url) return

    if (destination) {
      update.mutate(
        {
          id: destination.id,
          request: {
            name: name.trim(),
            config: url.trim() === "" ? undefined : url.trim(),
          },
        },
        { onSuccess: () => setOpen(false) }
      )
      return
    }

    create.mutate(
      { name: name.trim(), type: "DISCORD_WEBHOOK", config: url.trim() },
      {
        onSuccess: (created) => {
          setOpen(false)
          onCreated?.(created)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button type="button" variant="highlighted" size="sm">
            <Plus />
            Add destination
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>
              {editing ? "Edit destination" : "Add destination"}
            </DialogTitle>
            <DialogDescription>
              Alerts are posted to a Discord channel through a webhook. Create
              one under Channel settings, Integrations, Webhooks.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="destination-name">Name</Label>
              <Input
                id="destination-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                aria-invalid={errors.name ? true : undefined}
                disabled={pending}
                autoFocus
                placeholder="#ops-alerts"
              />
              <FormFieldError error={errors.name} />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="destination-url">Discord webhook URL</Label>
              <Input
                id="destination-url"
                type="url"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                aria-invalid={errors.url ? true : undefined}
                disabled={pending}
                autoComplete="off"
                placeholder={
                  editing
                    ? `${destination.maskedUrl} (leave blank to keep)`
                    : "https://discord.com/api/webhooks/…"
                }
              />
              <FormFieldError error={errors.url} />
            </div>
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
              {editing ? "Save" : "Add destination"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export { DestinationDialog }
