import { Download, LineChart, Plus, Server } from "lucide-react"
import type { LucideIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { CreateServerDialog } from "@/components/user/create-server-dialog"

const STEPS: { icon: LucideIcon; title: string; body: string }[] = [
  {
    icon: Plus,
    title: "Create a server",
    body: "Give the host a name. You get an ingest token right away.",
  },
  {
    icon: Download,
    title: "Install the agent",
    body: "Paste one command on the host. It starts reporting within seconds.",
  },
  {
    icon: LineChart,
    title: "Watch it live",
    body: "CPU, memory, disk, network and more appear as soon as data arrives.",
  },
]

function FirstServerPrompt() {
  return (
    <div className="flex flex-col items-center gap-8 rounded-lg border border-dashed border-border bg-card px-6 py-12 text-center">
      <div className="flex flex-col items-center gap-3">
        <span
          aria-hidden
          className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-brand"
        >
          <Server className="size-6" />
        </span>
        <h2 className="text-xl">Add your first server</h2>
        <p className="max-w-md text-muted-foreground">
          Connect a host to see live health, usage trends and outage history in
          one place.
        </p>
      </div>

      <ol className="grid w-full max-w-3xl gap-4 text-left sm:grid-cols-3">
        {STEPS.map((step, index) => (
          <li
            key={step.title}
            className="flex flex-col gap-1.5 rounded-lg border border-border bg-background p-4"
          >
            <span className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <step.icon aria-hidden className="size-4 text-brand" />
              {index + 1}. {step.title}
            </span>
            <span className="text-sm text-muted-foreground">{step.body}</span>
          </li>
        ))}
      </ol>

      <CreateServerDialog
        trigger={
          <Button type="button" variant="brand">
            <Plus />
            Add your first server
          </Button>
        }
      />
    </div>
  )
}

export { FirstServerPrompt }
