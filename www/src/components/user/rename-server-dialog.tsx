import { Pencil } from "lucide-react"
import { useState } from "react"

import { FormFieldError } from "@/components/form-field-error"
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
import { useRenameServer } from "@/hooks/use-rename-server"
import { MAX_SERVER_NAME_LENGTH } from "@/lib/server-name"

type RenameServerDialogProps = {
  serverId: number
  currentName: string
}

function RenameServerDialog({
  serverId,
  currentName,
}: RenameServerDialogProps) {
  const [open, setOpen] = useState(false)

  const {
    name,
    setName,
    fieldError,
    inputId,
    mutation,
    resetForm,
    submit,
  } = useRenameServer({
    serverId,
    currentName,
    onSuccess: () => setOpen(false),
  })

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }

    setOpen(nextOpen)

    resetForm()
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    submit(event, { onUnchanged: () => setOpen(false) })
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
              className="text-muted-foreground hover:bg-transparent hover:text-muted-foreground dark:text-muted-foreground dark:hover:bg-transparent dark:hover:text-white"
              aria-label={`Rename ${currentName}`}
            >
              <Pencil className="size-4" />
            </Button>
          </DialogTrigger>
        </TooltipTrigger>
        <TooltipContent>Rename server</TooltipContent>
      </Tooltip>
      <DialogContent className="rounded-sm border border-border sm:max-w-lg">
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
            <FormFieldError error={fieldError} />
          </div>

          <DialogFooter>
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
