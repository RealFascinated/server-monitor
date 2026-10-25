import { useQuery } from "@tanstack/react-query"

import { AlertBuilder } from "@/components/alerts/alert-builder"
import { QueryStatusShell } from "@/components/query-status-shell"
import { useAlertGroup } from "@/hooks/use-alert-groups"
import { useAlertRule } from "@/hooks/use-alert-rules"
import { alertMetricsQueryOptions } from "@/lib/api/user/alerts.queries"

/** Loads everything the form starts from, so its initial state is complete when it mounts. */
function AlertBuilderView({
  groupId,
  ruleId,
  initialMetric,
}: {
  groupId: number
  ruleId?: number
  initialMetric?: string
}) {
  const group = useAlertGroup(groupId)
  const rule = useAlertRule(ruleId ?? 0)
  const metrics = useQuery(alertMetricsQueryOptions())
  const editing = ruleId != null

  return (
    <QueryStatusShell
      error={group.error ?? metrics.error ?? (editing ? rule.error : null)}
      isPending={
        group.isPending || metrics.isPending || (editing && rule.isPending)
      }
      loadingMessage="Loading…"
      fallbackMessage="Could not load this alert."
      fallbackTitle="Alert unavailable"
    >
      {group.data && metrics.data && (!editing || rule.data) ? (
        <AlertBuilder
          group={group.data}
          metrics={metrics.data}
          rule={editing ? rule.data : undefined}
          initialMetric={initialMetric}
        />
      ) : null}
    </QueryStatusShell>
  )
}

export { AlertBuilderView }
