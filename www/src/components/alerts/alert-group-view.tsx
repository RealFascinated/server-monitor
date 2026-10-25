import { useQuery } from "@tanstack/react-query"
import { Link, useNavigate } from "@tanstack/react-router"
import { History, Pencil, Server, Trash2, Zap } from "lucide-react"

import { AddAlertDialog } from "@/components/alerts/add-alert-dialog"
import { AlertEventRow } from "@/components/alerts/alert-event-row"
import { AlertRulesTable } from "@/components/alerts/alert-rules-table"
import { GroupDialog } from "@/components/alerts/group-dialog"
import { GroupEnabledToggle } from "@/components/alerts/group-enabled-toggle"
import { Breadcrumb } from "@/components/breadcrumb"
import { Callout } from "@/components/callout"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { EmptyState } from "@/components/empty-state"
import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Button } from "@/components/ui/button"
import { useAlertGroup, useDeleteAlertGroup } from "@/hooks/use-alert-groups"
import { useAlertRules } from "@/hooks/use-alert-rules"
import { alertEventsQueryOptions } from "@/lib/api/user/alerts.queries"
import { formatCount } from "@/lib/formatter"
import { attentionCountColorClass } from "@/lib/metrics/percent-level"

const RECENT_EVENT_COUNT = 5

function SectionHeading({
  icon: Icon,
  title,
  action,
}: {
  icon: typeof Zap
  title: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <h2 className="flex items-center gap-2 text-sm font-semibold text-foreground">
        <Icon aria-hidden className="size-4 text-brand" />
        {title}
      </h2>
      {action}
    </div>
  )
}

function RecentActivity({ groupId }: { groupId: number }) {
  const { data } = useQuery(
    alertEventsQueryOptions({ page: 1, count: RECENT_EVENT_COUNT }, { groupId })
  )

  return (
    <div className="flex flex-col gap-3">
      <SectionHeading
        icon={History}
        title="Recent activity"
        action={
          <Button asChild variant="ghost" size="sm">
            <Link to="/alerts/history" search={{ groupId }}>
              View all
            </Link>
          </Button>
        }
      />
      {data && data.items.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {data.items.map((event) => (
            <AlertEventRow key={event.id} event={event} />
          ))}
        </ul>
      ) : (
        <p className="text-sm text-muted-foreground">
          Nothing has fired for this group yet.
        </p>
      )}
    </div>
  )
}

function AlertGroupView({ groupId }: { groupId: number }) {
  const navigate = useNavigate()
  const group = useAlertGroup(groupId)
  const rules = useAlertRules(groupId)
  const remove = useDeleteAlertGroup()

  return (
    <QueryStatusShell
      error={group.error ?? rules.error}
      isPending={group.isPending || rules.isPending}
      loadingMessage="Loading group…"
      fallbackMessage="Could not load this alert group."
      fallbackTitle="Group unavailable"
    >
      {group.data ? (
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-3">
            <Breadcrumb
              items={[
                { label: "Groups", to: "/alerts/groups" },
                { label: group.data.name, current: true },
              ]}
            />
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex min-w-0 flex-col gap-0.5">
                <h2 className="flex items-center gap-2 truncate text-xl font-semibold text-foreground">
                  {group.data.name}
                </h2>
                <p className="text-sm text-muted-foreground">
                  Every server in this group inherits every alert below.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">
                  {group.data.enabled ? "Enabled" : "Disabled"}
                </span>
                <GroupEnabledToggle group={group.data} />
                <GroupDialog
                  group={group.data}
                  trigger={
                    <Button type="button" variant="outline" size="sm">
                      <Pencil />
                      Edit group
                    </Button>
                  }
                />
                {group.data.defaultGroup ? null : (
                  <ConfirmDialog
                    trigger={
                      <Button type="button" variant="destructive" size="sm">
                        <Trash2 />
                        Delete
                      </Button>
                    }
                    title={`Delete ${group.data.name}?`}
                    description="This deletes the group and all of its alerts. The servers themselves are not affected."
                    confirmLabel="Delete"
                    confirmVariant="destructive"
                    onConfirm={async () => {
                      await remove.mutateAsync(groupId)
                      await navigate({ to: "/alerts/groups" })
                    }}
                  />
                )}
              </div>
            </div>
          </div>

          {group.data.enabled ? null : (
            <Callout type="warning" title="This group is disabled">
              None of its alerts are being checked and nothing will be sent.
              Turn the group on with the switch above to start monitoring its
              servers.
            </Callout>
          )}

          <div className="metric-stat-grid">
            <MetricStatCard
              title="Servers"
              value={group.data.servers.length}
              formatValue={(value) => formatCount(Math.round(value))}
              detail={
                group.data.servers.length === 0
                  ? "Nothing is being checked"
                  : "Inherit these alerts"
              }
              valueClassName={
                group.data.servers.length === 0 ? "text-warning" : undefined
              }
              animate={false}
            />
            <MetricStatCard
              title="Alerts"
              value={group.data.ruleCount}
              formatValue={(value) => formatCount(Math.round(value))}
              detail={`${rules.data?.filter((rule) => rule.enabled).length ?? 0} enabled`}
              animate={false}
            />
            <MetricStatCard
              title="Firing now"
              value={group.data.firingCount}
              formatValue={(value) => formatCount(Math.round(value))}
              detail={
                group.data.firingCount === 0
                  ? "Everything is healthy"
                  : "Needs attention"
              }
              valueClassName={attentionCountColorClass(group.data.firingCount)}
              animate={false}
            />
          </div>

          <div className="flex flex-col gap-3">
            <SectionHeading icon={Server} title="Servers" />
            {group.data.servers.length > 0 ? (
              <ul className="flex flex-wrap gap-2">
                {group.data.servers.map((server) => (
                  <li key={server.id}>
                    <Link
                      to="/servers/$serverId"
                      params={{ serverId: String(server.id) }}
                      className="rounded-full border border-border bg-card px-3 py-1 text-sm text-foreground transition-colors hover:border-primary/50"
                    >
                      {server.name}
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">
                No servers in this group yet, so its alerts aren&apos;t checking
                anything.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <SectionHeading
              icon={Zap}
              title="Alerts"
              action={<AddAlertDialog groupId={groupId} />}
            />
            {rules.data && rules.data.length > 0 ? (
              <AlertRulesTable rules={rules.data} />
            ) : (
              <EmptyState
                icon={Zap}
                title="No alerts in this group"
                description="Add an alert and it will apply to every server in the group."
                action={<AddAlertDialog groupId={groupId} />}
              />
            )}
          </div>

          <RecentActivity groupId={groupId} />
        </div>
      ) : null}
    </QueryStatusShell>
  )
}

export { AlertGroupView }
