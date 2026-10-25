import { Pencil, Send, Trash2, Webhook } from "lucide-react"

import { DestinationDialog } from "@/components/alerts/destination-dialog"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { EmptyState } from "@/components/empty-state"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import {
  useAlertDestinations,
  useDeleteAlertDestination,
  useTestAlertDestination,
} from "@/hooks/use-alert-destinations"
import type { AlertDestination } from "@/lib/api/user/alerts"

function ruleCountLabel(count: number): string {
  if (count === 0) return "Not used by any rule"
  return count === 1 ? "Used by 1 rule" : `Used by ${count} rules`
}

function DestinationRow({ destination }: { destination: AlertDestination }) {
  const test = useTestAlertDestination()
  const remove = useDeleteAlertDestination()
  const testing = test.isPending

  return (
    <li className="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 sm:flex-row sm:items-center">
      <span
        aria-hidden
        className="hidden size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-brand sm:flex"
      >
        <Webhook className="size-4" />
      </span>

      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <p className="truncate text-sm font-semibold text-foreground">
          {destination.name}
        </p>
        <p className="truncate font-mono text-xs text-muted-foreground">
          {destination.maskedUrl}
        </p>
        <p className="text-xs text-muted-foreground">
          Discord · {ruleCountLabel(destination.ruleCount)}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={testing}
          onClick={() => test.mutate(destination.id)}
        >
          {testing ? <Spinner /> : <Send />}
          Send test
        </Button>
        <DestinationDialog
          destination={destination}
          trigger={
            <Button type="button" variant="outline" size="sm">
              <Pencil />
              Edit
            </Button>
          }
        />
        <ConfirmDialog
          trigger={
            <Button type="button" variant="destructive" size="sm">
              <Trash2 />
              Delete
            </Button>
          }
          title={`Delete ${destination.name}?`}
          description={
            destination.ruleCount > 0
              ? `${ruleCountLabel(destination.ruleCount)}. Deleting this destination also deletes ${
                  destination.ruleCount === 1 ? "that rule" : "those rules"
                }.`
              : "Alerts will no longer be sent to this channel."
          }
          confirmLabel="Delete"
          confirmVariant="destructive"
          onConfirm={() =>
            remove.mutateAsync({
              id: destination.id,
              force: destination.ruleCount > 0,
            })
          }
        />
      </div>
    </li>
  )
}

function AlertDestinationsView() {
  const { data, error, isPending } = useAlertDestinations()

  return (
    <QueryStatusShell
      error={error}
      isPending={isPending}
      loadingMessage="Loading destinations…"
      fallbackMessage="Could not load alert destinations."
      fallbackTitle="Destinations unavailable"
    >
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            Where alerts are delivered. Rules without a destination still record
            history but notify no one.
          </p>
          <DestinationDialog />
        </div>

        {data && data.length > 0 ? (
          <ul className="flex flex-col gap-3">
            {data.map((destination) => (
              <DestinationRow key={destination.id} destination={destination} />
            ))}
          </ul>
        ) : (
          <EmptyState
            icon={Webhook}
            title="No destinations yet"
            description="Add a Discord webhook to get notified when an alert fires or recovers. The first one you add is attached to your existing rules."
            action={<DestinationDialog />}
          />
        )}
      </div>
    </QueryStatusShell>
  )
}

export { AlertDestinationsView }
