import { useNavigate } from "@tanstack/react-router"
import { Plus, SlidersHorizontal } from "lucide-react"
import { useState } from "react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { ALERT_TEMPLATES } from "@/lib/alerts/templates"

const optionClassName =
  "flex items-start gap-3 rounded-lg border border-border bg-card p-3 text-left transition-colors hover:border-primary/50 hover:bg-card-hover focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"

/** First step of adding an alert: pick a common starting point, or build one from scratch. */
function AddAlertDialog({ groupId }: { groupId: number }) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  function start(metric?: string) {
    setOpen(false)
    void navigate({
      to: "/alerts/groups/$groupId/rules/new",
      params: { groupId: String(groupId) },
      search: { metric },
    })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" variant="highlighted" size="sm">
          <Plus />
          Add alert
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Add an alert</DialogTitle>
          <DialogDescription>
            Start from a common alert, then adjust it. It applies to every
            server in the group.
          </DialogDescription>
        </DialogHeader>

        <ul className="grid gap-2 sm:grid-cols-2">
          {ALERT_TEMPLATES.map((template) => (
            <li key={template.id}>
              <button
                type="button"
                className={`${optionClassName} w-full`}
                onClick={() => start(template.metric)}
              >
                <span
                  aria-hidden
                  className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-brand"
                >
                  <template.icon className="size-4" />
                </span>
                <span className="flex flex-col">
                  <span className="text-sm font-semibold text-foreground">
                    {template.title}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {template.description}
                  </span>
                </span>
              </button>
            </li>
          ))}
          <li>
            <button
              type="button"
              className={`${optionClassName} w-full border-dashed`}
              onClick={() => start()}
            >
              <span
                aria-hidden
                className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground"
              >
                <SlidersHorizontal className="size-4" />
              </span>
              <span className="flex flex-col">
                <span className="text-sm font-semibold text-foreground">
                  Custom alert
                </span>
                <span className="text-xs text-muted-foreground">
                  Pick any metric yourself
                </span>
              </span>
            </button>
          </li>
        </ul>
      </DialogContent>
    </Dialog>
  )
}

export { AddAlertDialog }
