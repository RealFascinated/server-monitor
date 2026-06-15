import { useState } from "react"

import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import type { buttonVariants } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import type { VariantProps } from "class-variance-authority"

type ConfirmVariant = NonNullable<
  VariantProps<typeof buttonVariants>["variant"]
>

type ConfirmDialogProps = {
  trigger: React.ReactNode
  triggerTooltip?: string
  title: string
  description: React.ReactNode
  confirmLabel?: string
  cancelLabel?: string
  confirmVariant?: ConfirmVariant
  onConfirm: () => void | Promise<void>
  onOpenChange?: (open: boolean) => void
}

function ConfirmDialog({
  trigger,
  triggerTooltip,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  confirmVariant = "highlighted",
  onConfirm,
  onOpenChange,
}: ConfirmDialogProps) {
  const [open, setOpen] = useState(false)
  const [isPending, setIsPending] = useState(false)

  async function handleOpenChange(nextOpen: boolean) {
    if (isPending) {
      return
    }

    setOpen(nextOpen)
    onOpenChange?.(nextOpen)
  }

  async function handleConfirm() {
    setIsPending(true)

    try {
      await onConfirm()
      setOpen(false)
    } catch {
      // Caller surfaces errors via toast.
    } finally {
      setIsPending(false)
    }
  }

  const dialogTrigger = triggerTooltip ? (
    <Tooltip>
      <TooltipTrigger asChild>
        <DialogTrigger asChild>{trigger}</DialogTrigger>
      </TooltipTrigger>
      <TooltipContent>{triggerTooltip}</TooltipContent>
    </Tooltip>
  ) : (
    <DialogTrigger asChild>{trigger}</DialogTrigger>
  )

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      {dialogTrigger}
      <DialogContent className="rounded-sm border border-neutral-200 sm:max-w-lg dark:border-monitor-gray-300">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <DialogFooter className="border-t border-neutral-200 pt-3 dark:border-monitor-gray-200">
          <Button
            type="button"
            variant="default"
            disabled={isPending}
            onClick={() => handleOpenChange(false)}
          >
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant={confirmVariant}
            disabled={isPending}
            onClick={handleConfirm}
          >
            {isPending ? <Spinner /> : null}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export { ConfirmDialog }
