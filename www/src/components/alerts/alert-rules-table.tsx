import {
  getCoreRowModel,
  useLegacyTable as useReactTable,
} from "@tanstack/react-table/legacy"
import type { LegacyColumnDef as ColumnDef } from "@tanstack/react-table/legacy"
import { Link } from "@tanstack/react-router"
import { Copy, Pencil, Trash2, Webhook } from "lucide-react"
import { useMemo } from "react"

import { AlertSeverityBadge } from "@/components/alerts/alert-severity-badge"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { SettingsToggle } from "@/components/ui/settings-toggle"
import {
  useCreateAlertRule,
  useDeleteAlertRule,
  useSetAlertRuleEnabled,
} from "@/hooks/use-alert-rules"
import { useTemperatureUnit } from "@/hooks/use-temperature-unit"
import { ruleToCopyRequest } from "@/lib/alerts/draft"
import { describeCondition, describeLabelFilter } from "@/lib/alerts/summary"
import type { AlertRule } from "@/lib/api/user/alerts"
import { cn } from "@/lib/utils"

function ruleTitle(rule: AlertRule): string {
  return rule.name || rule.metricLabel
}

function RuleName({ rule, linked }: { rule: AlertRule; linked: boolean }) {
  const filter = describeLabelFilter(rule.labelFilter)

  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <div className="flex items-center gap-2">
        {linked ? (
          <Link
            to="/alerts/groups/$groupId/rules/$ruleId"
            params={{ groupId: String(rule.groupId), ruleId: String(rule.id) }}
            className="truncate font-medium text-foreground hover:text-brand"
          >
            {ruleTitle(rule)}
          </Link>
        ) : (
          <span className="truncate font-medium text-foreground">
            {ruleTitle(rule)}
          </span>
        )}
        <AlertSeverityBadge severity={rule.severity} />
      </div>
      {filter ? (
        <span className="truncate font-mono text-xs text-muted-foreground">
          {filter}
        </span>
      ) : null}
    </div>
  )
}

function RuleCondition({ rule }: { rule: AlertRule }) {
  const { temperatureUnit } = useTemperatureUnit()

  return (
    <span className="text-muted-foreground">
      {describeCondition(
        {
          metricLabel: rule.metricLabel,
          unit: rule.unit,
          operator: rule.operator,
          threshold: rule.threshold,
          windowSeconds: rule.windowSeconds,
        },
        temperatureUnit
      )}
    </span>
  )
}

function RuleDestination({ rule }: { rule: AlertRule }) {
  if (!rule.destinationName) {
    return (
      <SimpleTooltip content="This alert is recorded in history but notifies no one.">
        <span className="tooltip-hint text-warning">Nobody</span>
      </SimpleTooltip>
    )
  }

  return (
    <span className="flex items-center gap-1.5">
      <Webhook aria-hidden className="size-3.5 text-muted-foreground" />
      <span className="truncate">{rule.destinationName}</span>
    </span>
  )
}

function RuleState({ rule }: { rule: AlertRule }) {
  if (!rule.enabled) {
    return <span className="text-muted-foreground">Disabled</span>
  }
  const firing = rule.firingCount > 0

  return (
    <span
      className={cn(
        "inline-flex rounded-sm px-2 py-0.5 text-xs font-medium",
        firing
          ? "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300"
          : "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300"
      )}
    >
      {firing ? `${rule.firingCount} firing` : "OK"}
    </span>
  )
}

function RuleActions({ rule }: { rule: AlertRule }) {
  const setEnabled = useSetAlertRuleEnabled()
  const duplicate = useCreateAlertRule()
  const remove = useDeleteAlertRule()
  const title = ruleTitle(rule)

  return (
    <div className="flex items-center justify-end gap-1">
      <SettingsToggle
        ariaLabel={`${rule.enabled ? "Disable" : "Enable"} ${title}`}
        checked={rule.enabled}
        disabled={setEnabled.isPending}
        onCheckedChange={(enabled) =>
          setEnabled.mutate({ id: rule.id, enabled })
        }
      />
      <Button
        asChild
        type="button"
        variant="ghost"
        size="icon"
        aria-label={`Edit ${title}`}
      >
        <Link
          to="/alerts/groups/$groupId/rules/$ruleId"
          params={{ groupId: String(rule.groupId), ruleId: String(rule.id) }}
        >
          <Pencil />
        </Link>
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label={`Duplicate ${title}`}
        disabled={duplicate.isPending}
        onClick={() => duplicate.mutate(ruleToCopyRequest(rule))}
      >
        <Copy />
      </Button>
      <ConfirmDialog
        trigger={
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={`Delete ${title}`}
          >
            <Trash2 />
          </Button>
        }
        title={`Delete ${title}?`}
        description="Every server in the group stops being checked for this alert, and its history is removed."
        confirmLabel="Delete"
        confirmVariant="destructive"
        onConfirm={() => remove.mutateAsync(rule.id)}
      />
    </div>
  )
}

type AlertRulesTableProps = {
  rules: AlertRule[]
  /** Hides the controls for places that only show what a group configures. */
  readOnly?: boolean
}

function AlertRulesTable({ rules, readOnly = false }: AlertRulesTableProps) {
  const columns = useMemo<ColumnDef<AlertRule>[]>(
    () => [
      {
        id: "alert",
        header: "Alert",
        cell: ({ row }) => <RuleName rule={row.original} linked={!readOnly} />,
      },
      {
        id: "condition",
        header: "Condition",
        cell: ({ row }) => <RuleCondition rule={row.original} />,
      },
      {
        id: "notifies",
        header: "Notifies",
        cell: ({ row }) => <RuleDestination rule={row.original} />,
      },
      {
        id: "state",
        header: "State",
        cell: ({ row }) => <RuleState rule={row.original} />,
      },
      ...(readOnly
        ? []
        : [
            {
              id: "actions",
              header: () => <span className="sr-only">Actions</span>,
              cell: ({ row }: { row: { original: AlertRule } }) => (
                <RuleActions rule={row.original} />
              ),
            },
          ]),
    ],
    [readOnly]
  )

  const table = useReactTable({
    data: rules,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  return <DataTable table={table} />
}

export { AlertRulesTable }
