import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { Bell, History, Users } from "lucide-react"

import { AlertEventsTable } from "@/components/alerts/alert-events-table"
import { AlertRulesTable } from "@/components/alerts/alert-rules-table"
import { EmptyState } from "@/components/empty-state"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Button } from "@/components/ui/button"
import { useAlertGroups } from "@/hooks/use-alert-groups"
import { useAlertRules } from "@/hooks/use-alert-rules"
import { alertEventsQueryOptions } from "@/lib/api/user/alerts.queries"

const RECENT_EVENT_COUNT = 5

function Section({
  icon: Icon,
  title,
  description,
  children,
}: {
  icon: typeof Bell
  title: string
  description?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-0.5">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Icon aria-hidden className="size-4 text-brand" />
          {title}
        </h2>
        {description ? (
          <p className="text-xs text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {children}
    </div>
  )
}

function ServerRecentActivity({ serverId }: { serverId: number }) {
  const { data } = useQuery(
    alertEventsQueryOptions(
      { page: 1, count: RECENT_EVENT_COUNT },
      { serverId }
    )
  )

  if (!data || data.items.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Nothing has fired for this server yet.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <AlertEventsTable events={data.items} />
      <Button asChild variant="ghost" size="sm" className="w-fit">
        <Link to="/alerts/history" search={{ serverId }}>
          View all
        </Link>
      </Button>
    </div>
  )
}

/**
 * What alerts a server has, read-only: groups own the configuration, so membership and rules are
 * edited from the group's page rather than here.
 */
function ServerAlertsView({ serverId }: { serverId: number }) {
  const groups = useAlertGroups()
  const rules = useAlertRules()

  const memberOf =
    groups.data?.filter((group) =>
      group.servers.some((server) => server.id === serverId)
    ) ?? []
  const activeIds = new Set(
    memberOf.filter((group) => group.enabled).map((group) => group.id)
  )
  const inherited = rules.data?.filter((rule) => activeIds.has(rule.groupId))

  return (
    <QueryStatusShell
      error={groups.error ?? rules.error}
      isPending={groups.isPending || rules.isPending}
      loadingMessage="Loading alerts…"
      fallbackMessage="Could not load alerts."
      fallbackTitle="Alerts unavailable"
    >
      <div className="flex flex-col gap-6">
        <Section
          icon={Users}
          title="Alert groups"
          description="Alerts are configured on groups. This server inherits the alerts of every enabled group it is in."
        >
          {memberOf.length > 0 ? (
            <ul className="flex flex-wrap gap-2">
              {memberOf.map((group) => (
                <li key={group.id}>
                  <Link
                    to="/alerts/groups/$groupId"
                    params={{ groupId: String(group.id) }}
                    className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-sm transition-colors hover:border-primary/50"
                  >
                    <span className="font-medium text-foreground">
                      {group.name}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {group.enabled
                        ? `${group.ruleCount} ${group.ruleCount === 1 ? "alert" : "alerts"}`
                        : "Disabled"}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-muted-foreground">
              This server is not in any alert group, so nothing is being
              checked.
            </p>
          )}
        </Section>

        <Section icon={Bell} title="Inherited alerts">
          {inherited && inherited.length > 0 ? (
            <AlertRulesTable rules={inherited} readOnly />
          ) : (
            <EmptyState
              icon={Bell}
              title="No alerts apply"
              description={
                memberOf.length === 0
                  ? "Add this server to an alert group to start monitoring it."
                  : "The groups this server is in are disabled or have no alerts."
              }
              action={
                <Button asChild variant="outline" size="sm">
                  <Link to="/alerts/groups">Manage alert groups</Link>
                </Button>
              }
            />
          )}
        </Section>

        <Section icon={History} title="Recent activity">
          <ServerRecentActivity serverId={serverId} />
        </Section>
      </div>
    </QueryStatusShell>
  )
}

export { ServerAlertsView }
