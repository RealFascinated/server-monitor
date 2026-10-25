import { useQuery } from "@tanstack/react-query"
import { History } from "lucide-react"

import { AlertEventRow } from "@/components/alerts/alert-event-row"
import { EmptyState } from "@/components/empty-state"
import { Pagination } from "@/components/pagination"
import { QueryStatusShell } from "@/components/query-status-shell"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useAlertGroups } from "@/hooks/use-alert-groups"
import { useUserServers } from "@/hooks/use-user-servers"
import type { AlertEventFilters, AlertEventState } from "@/lib/api/user/alerts"
import { alertEventsQueryOptions } from "@/lib/api/user/alerts.queries"
import type { PageSearchParams } from "@/lib/schemas/pagination"

const ALL = "all"

type HistoryFilters = Pick<AlertEventFilters, "state" | "groupId" | "serverId">

type AlertHistoryViewProps = {
  pagination: PageSearchParams
  filters: HistoryFilters
  onFiltersChange: (next: HistoryFilters) => void
  onPageChange: (page: number) => void
  onPageSizeChange: (count: number) => void
}

function FilterSelect({
  label,
  value,
  allLabel,
  options,
  onChange,
}: {
  label: string
  value: string
  allLabel: string
  options: { value: string; label: string }[]
  onChange: (value: string) => void
}) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-full sm:w-44" aria-label={label}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={ALL}>{allLabel}</SelectItem>
        {options.map((option) => (
          <SelectItem key={option.value} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

function AlertHistoryView({
  pagination,
  filters,
  onFiltersChange,
  onPageChange,
  onPageSizeChange,
}: AlertHistoryViewProps) {
  const groups = useAlertGroups()
  const servers = useUserServers()
  const { data, error, isPending } = useQuery(
    alertEventsQueryOptions(pagination, filters)
  )
  const filtered =
    filters.state != null || filters.groupId != null || filters.serverId != null

  function numberOrUndefined(value: string): number | undefined {
    return value === ALL ? undefined : Number(value)
  }

  return (
    <QueryStatusShell
      error={error}
      isPending={isPending}
      loadingMessage="Loading history…"
      fallbackMessage="Could not load alert history."
      fallbackTitle="History unavailable"
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            Every time one of your alerts fired, on any server.
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <FilterSelect
              label="Status"
              value={filters.state ?? ALL}
              allLabel="Any status"
              options={[
                { value: "firing", label: "Firing" },
                { value: "resolved", label: "Resolved" },
              ]}
              onChange={(value) =>
                onFiltersChange({
                  ...filters,
                  state: value === ALL ? undefined : (value as AlertEventState),
                })
              }
            />
            <FilterSelect
              label="Group"
              value={filters.groupId == null ? ALL : String(filters.groupId)}
              allLabel="Any group"
              options={(groups.data ?? []).map((group) => ({
                value: String(group.id),
                label: group.name,
              }))}
              onChange={(value) =>
                onFiltersChange({
                  ...filters,
                  groupId: numberOrUndefined(value),
                })
              }
            />
            <FilterSelect
              label="Server"
              value={filters.serverId == null ? ALL : String(filters.serverId)}
              allLabel="Any server"
              options={(servers.data ?? []).map((server) => ({
                value: String(server.serverId),
                label: server.serverName,
              }))}
              onChange={(value) =>
                onFiltersChange({
                  ...filters,
                  serverId: numberOrUndefined(value),
                })
              }
            />
          </div>
        </div>

        {data && data.items.length > 0 ? (
          <>
            <ul className="flex flex-col gap-3">
              {data.items.map((event) => (
                <AlertEventRow key={event.id} event={event} />
              ))}
            </ul>
            <Pagination
              page={data}
              currentPage={pagination.page}
              pageSize={pagination.count}
              onPageChange={onPageChange}
              onPageSizeChange={onPageSizeChange}
              itemLabel="event"
            />
          </>
        ) : (
          <EmptyState
            icon={History}
            title="No alert events"
            description={
              filtered
                ? "No events match these filters."
                : "When an alert fires it will show up here, along with when it recovered."
            }
          />
        )}
      </div>
    </QueryStatusShell>
  )
}

export { AlertHistoryView }
