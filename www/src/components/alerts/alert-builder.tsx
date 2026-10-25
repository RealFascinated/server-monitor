import { useQuery } from "@tanstack/react-query"
import { Link, useNavigate } from "@tanstack/react-router"
import { ChevronDown, Send } from "lucide-react"
import { useMemo, useState } from "react"

import { AlertPreviewChart } from "@/components/alerts/alert-preview-chart"
import { ChoiceChips } from "@/components/alerts/choice-chips"
import { DestinationDialog } from "@/components/alerts/destination-dialog"
import { MetricPicker } from "@/components/alerts/metric-picker"
import { Breadcrumb } from "@/components/breadcrumb"
import { CollapsiblePanel } from "@/components/collapsible-panel"
import { FormFieldError } from "@/components/form-field-error"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { SettingsToggle } from "@/components/ui/settings-toggle"
import {
  useAlertDestinations,
  useTestAlertDestination,
} from "@/hooks/use-alert-destinations"
import { useCreateAlertRule, useUpdateAlertRule } from "@/hooks/use-alert-rules"
import { useTemperatureUnit } from "@/hooks/use-temperature-unit"
import {
  EMPTY_DRAFT,
  draftForMetric,
  draftToRequest,
  isStateMetric,
  ruleToDraft,
  validateDraft,
} from "@/lib/alerts/draft"
import type { AlertDraft } from "@/lib/alerts/draft"
import {
  OPERATOR_LABELS,
  REPEAT_OPTIONS,
  SEVERITY_LABELS,
  WINDOW_OPTIONS,
  describeCondition,
  describeLabelFilter,
  formatWindow,
} from "@/lib/alerts/summary"
import { alertInputSuffix, fromAlertInputValue } from "@/lib/alerts/units"
import type {
  AlertGroup,
  AlertMetric,
  AlertOperator,
  AlertRule,
  AlertSeverity,
} from "@/lib/api/user/alerts"
import { alertSeriesQueryOptions } from "@/lib/api/user/alerts.queries"

const ANY = "__any__"
const NO_DESTINATION = "__none__"

const OPERATOR_OPTIONS = (Object.keys(OPERATOR_LABELS) as AlertOperator[]).map(
  (operator) => ({ value: operator, label: `Is ${OPERATOR_LABELS[operator]}` })
)
const SEVERITY_OPTIONS = (Object.keys(SEVERITY_LABELS) as AlertSeverity[]).map(
  (severity) => ({ value: severity, label: SEVERITY_LABELS[severity] })
)
const WINDOW_CHOICES = WINDOW_OPTIONS.map(({ value, label }) => ({
  value,
  label,
}))

function Step({
  number,
  title,
  description,
  children,
}: {
  number: number
  title: string
  description?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <span
          aria-hidden
          className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-brand"
        >
          {number}
        </span>
        <div className="flex flex-col gap-0.5">
          <h3 className="text-sm font-semibold text-foreground">{title}</h3>
          {description ? (
            <p className="text-xs text-muted-foreground">{description}</p>
          ) : null}
        </div>
      </div>
      <div className="flex flex-col gap-4 sm:pl-9">{children}</div>
    </div>
  )
}

function Field({
  label,
  htmlFor,
  hint,
  error,
  children,
}: {
  label: string
  htmlFor?: string
  hint?: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
      <FormFieldError error={error} />
    </div>
  )
}

function ThresholdInput({
  id,
  value,
  suffix,
  placeholder,
  invalid,
  onChange,
}: {
  id: string
  value: string
  suffix: string
  placeholder?: string
  invalid?: boolean
  onChange: (value: string) => void
}) {
  return (
    <div className="relative">
      <Input
        id={id}
        inputMode="decimal"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={invalid ? true : undefined}
        className={suffix ? "pr-12" : undefined}
      />
      {suffix ? (
        <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs text-muted-foreground">
          {suffix}
        </span>
      ) : null}
    </div>
  )
}

function SeriesFilterFields({
  metric,
  serverId,
  value,
  onChange,
}: {
  metric: AlertMetric
  serverId: number | null
  value: Record<string, string>
  onChange: (value: Record<string, string>) => void
}) {
  const series = useQuery({
    ...alertSeriesQueryOptions(serverId ?? 0, metric.metric),
    enabled: serverId != null,
  })

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {metric.labelKeys.map((key) => {
        const options = [
          ...new Set(
            (series.data?.series ?? [])
              .map((labels) => labels[key])
              .filter((option) => option !== "")
          ),
        ]
        if (value[key] && !options.includes(value[key]))
          options.push(value[key])

        return (
          <Field key={key} label={`Only this ${key}`}>
            <Select
              value={value[key] ?? ANY}
              onValueChange={(next) => {
                const { [key]: _removed, ...rest } = value
                onChange(next === ANY ? rest : { ...rest, [key]: next })
              }}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ANY}>Every {key}</SelectItem>
                {options.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
        )
      })}
    </div>
  )
}

type AlertBuilderProps = {
  group: AlertGroup
  metrics: AlertMetric[]
  /** Editing when set, creating otherwise. */
  rule?: AlertRule
  /** Preselects a metric when creating, e.g. from a template. */
  initialMetric?: string
  /** Destination a new alert notifies until the user picks another. */
  defaultDestinationId?: number
}

function AlertBuilder({
  group,
  metrics,
  rule,
  initialMetric,
  defaultDestinationId,
}: AlertBuilderProps) {
  const navigate = useNavigate()
  const { temperatureUnit } = useTemperatureUnit()
  const destinations = useAlertDestinations()
  const create = useCreateAlertRule()
  const update = useUpdateAlertRule()
  const testDestination = useTestAlertDestination()

  const [draft, setDraft] = useState<AlertDraft>(() => {
    if (rule) return ruleToDraft(rule, temperatureUnit)
    const preset = metrics.find((metric) => metric.metric === initialMetric)
    const base = { ...EMPTY_DRAFT, destinationId: defaultDestinationId ?? null }
    return preset ? draftForMetric(base, preset, temperatureUnit) : base
  })
  const [showErrors, setShowErrors] = useState(false)
  const [advancedOpen, setAdvancedOpen] = useState(
    () =>
      rule?.recoveryThreshold != null || (rule?.repeatIntervalSeconds ?? 0) > 0
  )
  const [previewServerId, setPreviewServerId] = useState<number | null>(
    group.servers[0]?.id ?? null
  )

  const metric = useMemo(
    () => metrics.find((candidate) => candidate.metric === draft.metric),
    [metrics, draft.metric]
  )
  const unit = metric?.unit ?? rule?.unit
  const isState = isStateMetric(unit)
  const errors = validateDraft(draft, unit, temperatureUnit)
  const pending = create.isPending || update.isPending
  const hasDestinations = (destinations.data?.length ?? 0) > 0

  function patch(next: Partial<AlertDraft>) {
    setDraft((current) => ({ ...current, ...next }))
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setShowErrors(true)
    if (Object.keys(errors).length > 0 || !unit) return

    const request = draftToRequest(group.id, draft, unit, temperatureUnit)
    const done = () =>
      navigate({
        to: "/alerts/groups/$groupId",
        params: { groupId: String(group.id) },
      })

    if (rule) {
      update.mutate({ id: rule.id, request }, { onSuccess: done })
    } else {
      create.mutate(request, { onSuccess: done })
    }
  }

  const threshold =
    draft.threshold.trim() === "" || Number.isNaN(Number(draft.threshold))
      ? null
      : Number(draft.threshold)
  const storedThreshold =
    threshold != null && unit
      ? fromAlertInputValue(unit, threshold, temperatureUnit)
      : null
  const recoveryRaw = Number(draft.recoveryThreshold)
  const storedRecovery =
    draft.recoveryThreshold.trim() !== "" &&
    Number.isFinite(recoveryRaw) &&
    unit
      ? fromAlertInputValue(unit, recoveryRaw, temperatureUnit)
      : null

  const suffix = unit ? alertInputSuffix(unit, temperatureUnit) : ""
  const selectedDestination = destinations.data?.find(
    (destination) => destination.id === draft.destinationId
  )
  const condition =
    metric && storedThreshold != null && unit
      ? describeCondition(
          {
            metricLabel: metric.label,
            unit,
            operator: draft.operator,
            threshold: storedThreshold,
            windowSeconds: draft.windowSeconds,
          },
          temperatureUnit
        )
      : null
  const filterSummary = describeLabelFilter(draft.labelFilter)
  const serverCount = group.servers.length
  const repeatLabel = REPEAT_OPTIONS.find(
    (option) => option.value === draft.repeatIntervalSeconds
  )?.label

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <Breadcrumb
          items={[
            { label: "Groups", to: "/alerts/groups" },
            {
              label: group.name,
              to: "/alerts/groups/$groupId",
              params: { groupId: String(group.id) },
            },
            { label: rule ? "Edit alert" : "New alert", current: true },
          ]}
        />
        <h2 className="text-xl font-semibold text-foreground">
          {rule ? "Edit alert" : "New alert"}
        </h2>
      </div>

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="flex flex-col gap-5">
          <Step
            number={1}
            title="What to watch"
            description="Pick the metric this alert checks on every server in the group."
          >
            <Field
              label="Metric"
              error={showErrors ? errors.metric : undefined}
            >
              <MetricPicker
                metrics={metrics}
                value={draft.metric}
                invalid={showErrors && errors.metric != null}
                onChange={(next) =>
                  setDraft(draftForMetric(draft, next, temperatureUnit))
                }
              />
            </Field>

            {metric && metric.labelKeys.length > 0 ? (
              <SeriesFilterFields
                metric={metric}
                serverId={previewServerId}
                value={draft.labelFilter}
                onChange={(labelFilter) => patch({ labelFilter })}
              />
            ) : null}
          </Step>

          <Step
            number={2}
            title="When to alert"
            description={
              isState
                ? "This metric is either healthy or not, so there is nothing to tune."
                : "The alert fires when the condition holds and resolves when it stops."
            }
          >
            {isState ? (
              <p className="rounded-md bg-muted px-3 py-2 text-sm text-foreground">
                Fires as soon as <strong>{metric?.label}</strong> reports
                unhealthy, and resolves when it recovers.
              </p>
            ) : (
              <>
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Condition">
                    <Select
                      value={draft.operator}
                      onValueChange={(value) =>
                        patch({ operator: value as AlertOperator })
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {OPERATOR_OPTIONS.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field
                    label="Threshold"
                    htmlFor="alert-threshold"
                    error={showErrors ? errors.threshold : undefined}
                  >
                    <ThresholdInput
                      id="alert-threshold"
                      value={draft.threshold}
                      suffix={suffix}
                      invalid={showErrors && errors.threshold != null}
                      onChange={(value) => patch({ threshold: value })}
                    />
                  </Field>
                </div>

                <Field
                  label="Averaged over"
                  hint="Spikes shorter than this are ignored. Instant reacts to every sample."
                >
                  <ChoiceChips
                    label="Averaged over"
                    value={draft.windowSeconds}
                    options={WINDOW_CHOICES}
                    onChange={(windowSeconds) => patch({ windowSeconds })}
                  />
                </Field>
              </>
            )}

            <Field label="Severity">
              <ChoiceChips
                label="Severity"
                value={draft.severity}
                options={SEVERITY_OPTIONS}
                onChange={(severity) => patch({ severity })}
              />
            </Field>

            {isState ? null : (
              <>
                <button
                  type="button"
                  onClick={() => setAdvancedOpen((open) => !open)}
                  aria-expanded={advancedOpen}
                  className="flex w-fit items-center gap-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
                >
                  <ChevronDown
                    aria-hidden
                    className={`size-4 transition-transform ${advancedOpen ? "rotate-180" : ""}`}
                  />
                  Advanced
                </button>
                <CollapsiblePanel open={advancedOpen}>
                  <div className="flex flex-col gap-4 pt-1">
                    <Field
                      label="Recovery threshold"
                      htmlFor="alert-recovery"
                      hint="Resolve only once the value crosses back past this, so a value hovering at the threshold doesn't flap."
                      error={showErrors ? errors.recoveryThreshold : undefined}
                    >
                      <ThresholdInput
                        id="alert-recovery"
                        value={draft.recoveryThreshold}
                        suffix={suffix}
                        placeholder="Same as threshold"
                        invalid={showErrors && errors.recoveryThreshold != null}
                        onChange={(value) =>
                          patch({ recoveryThreshold: value })
                        }
                      />
                    </Field>
                    <Field label="Remind me while it keeps firing">
                      <ChoiceChips
                        label="Reminder interval"
                        value={draft.repeatIntervalSeconds}
                        options={REPEAT_OPTIONS}
                        onChange={(repeatIntervalSeconds) =>
                          patch({ repeatIntervalSeconds })
                        }
                      />
                    </Field>
                  </div>
                </CollapsiblePanel>
              </>
            )}
          </Step>

          <Step
            number={3}
            title="Who to tell"
            description="Where this alert is sent when it fires and when it recovers."
          >
            <div className="flex flex-wrap items-end gap-2">
              <div className="flex min-w-48 flex-1 flex-col gap-2">
                <Label>Destination</Label>
                <Select
                  value={
                    draft.destinationId == null
                      ? NO_DESTINATION
                      : String(draft.destinationId)
                  }
                  onValueChange={(value) =>
                    patch({
                      destinationId:
                        value === NO_DESTINATION ? null : Number(value),
                    })
                  }
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_DESTINATION}>
                      Nobody, record history only
                    </SelectItem>
                    {destinations.data?.map((destination) => (
                      <SelectItem
                        key={destination.id}
                        value={String(destination.id)}
                      >
                        {destination.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={!selectedDestination || testDestination.isPending}
                onClick={() =>
                  selectedDestination &&
                  testDestination.mutate(selectedDestination.id)
                }
              >
                {testDestination.isPending ? <Spinner /> : <Send />}
                Send test
              </Button>
              <DestinationDialog
                onCreated={(created) => patch({ destinationId: created.id })}
                trigger={
                  <Button type="button" variant="outline" size="sm">
                    {hasDestinations ? "New destination" : "Add a destination"}
                  </Button>
                }
              />
            </div>

            <Field
              label="Name (optional)"
              htmlFor="alert-name"
              error={showErrors ? errors.name : undefined}
            >
              <Input
                id="alert-name"
                value={draft.name}
                placeholder={metric?.label ?? "Alert name"}
                onChange={(event) => patch({ name: event.target.value })}
                aria-invalid={showErrors && errors.name ? true : undefined}
              />
            </Field>

            <div className="flex items-center justify-between gap-4">
              <Label htmlFor="alert-enabled">Enabled</Label>
              <SettingsToggle
                ariaLabel="Enabled"
                checked={draft.enabled}
                onCheckedChange={(enabled) => patch({ enabled })}
              />
            </div>
          </Step>
        </div>

        <aside className="flex flex-col gap-4 lg:sticky lg:top-4">
          <div className="flex flex-col gap-3 rounded-lg border border-border bg-card p-5">
            <h3 className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Summary
            </h3>
            {condition ? (
              <p className="text-sm text-foreground">
                Alert me when <strong>{condition}</strong>
                {filterSummary ? ` (${filterSummary})` : ""}, on{" "}
                {serverCount === 0
                  ? `any server in ${group.name}`
                  : serverCount === 1
                    ? `the 1 server in ${group.name}`
                    : `all ${serverCount} servers in ${group.name}`}
                .
              </p>
            ) : isState && metric ? (
              <p className="text-sm text-foreground">
                Alert me when <strong>{metric.label} is unhealthy</strong>, on{" "}
                {serverCount === 1
                  ? "the 1 server"
                  : `all ${serverCount} servers`}{" "}
                in {group.name}.
              </p>
            ) : (
              <p className="text-sm text-muted-foreground">
                Choose a metric and a threshold to see what this alert does.
              </p>
            )}
            <ul className="flex flex-col gap-1 text-xs text-muted-foreground">
              <li>
                {selectedDestination
                  ? `Notifies ${selectedDestination.name}.`
                  : "Notifies nobody. It still shows in history."}
              </li>
              {!isState && draft.windowSeconds > 0 ? (
                <li>
                  Needs {formatWindow(draft.windowSeconds)} of data before it
                  can fire.
                </li>
              ) : null}
              {!isState && repeatLabel && draft.repeatIntervalSeconds > 0 ? (
                <li>Reminds you {repeatLabel.toLowerCase()} while firing.</li>
              ) : null}
            </ul>
          </div>

          <div className="flex flex-col gap-3 rounded-lg border border-border bg-card p-5">
            <h3 className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Preview
            </h3>
            {group.servers.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Add a server to this group to preview the alert against real
                data.
              </p>
            ) : !metric || !unit ? (
              <p className="text-sm text-muted-foreground">
                Choose a metric to replay it over the last 7 days.
              </p>
            ) : (
              <>
                {group.servers.length > 1 ? (
                  <Select
                    value={String(previewServerId)}
                    onValueChange={(value) => setPreviewServerId(Number(value))}
                  >
                    <SelectTrigger className="w-full" aria-label="Preview on">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {group.servers.map((server) => (
                        <SelectItem key={server.id} value={String(server.id)}>
                          {server.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : null}
                {previewServerId != null ? (
                  <AlertPreviewChart
                    temperatureUnit={temperatureUnit}
                    condition={{
                      serverId: previewServerId,
                      metric: metric.metric,
                      labelFilter: draft.labelFilter,
                      operator: draft.operator,
                      threshold: storedThreshold,
                      recoveryThreshold: storedRecovery,
                      windowSeconds: draft.windowSeconds,
                      severity: draft.severity,
                      unit,
                      metricLabel: metric.label,
                    }}
                  />
                ) : null}
              </>
            )}
          </div>

          <div className="flex items-center justify-end gap-2">
            <Button asChild variant="outline">
              <Link
                to="/alerts/groups/$groupId"
                params={{ groupId: String(group.id) }}
              >
                Cancel
              </Link>
            </Button>
            <Button type="submit" variant="highlighted" disabled={pending}>
              {pending ? <Spinner /> : null}
              {rule ? "Save alert" : "Create alert"}
            </Button>
          </div>
        </aside>
      </div>
    </form>
  )
}

export { AlertBuilder }
