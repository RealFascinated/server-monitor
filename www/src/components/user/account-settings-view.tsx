import type { ReactNode } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { SettingsPageContent } from "@/components/settings/settings-page-content"
import { SettingsPreferenceRow } from "@/components/settings/settings-preference-row"
import { SettingsSectionHeader } from "@/components/settings/settings-section-header"
import { ThemeSwitcher } from "@/components/theme-switcher"
import { ChangePasswordForm } from "@/components/user/change-password-form"
import { UserRoleBadge } from "@/components/user/user-role-badge"
import { Input } from "@/components/ui/input"
import { SettingsToggle } from "@/components/ui/settings-toggle"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import { useMetricRefreshInterval } from "@/hooks/use-metric-refresh-interval"
import {
  SIDEBAR_COLUMNS,
  useSidebarColumnVisibility,
} from "@/hooks/use-sidebar-column-visibility"
import type { SidebarColumnId } from "@/hooks/use-sidebar-column-visibility"
import { useSidebarDetailedMode } from "@/hooks/use-sidebar-detailed-mode"
import type { User } from "@/lib/auth/types"
import { formatDate, formatDateWithRelative } from "@/lib/formatter"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { METRIC_REFRESH_INTERVAL_OPTIONS } from "@/lib/metrics/refresh-interval"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { METRIC_RANGE_OPTIONS } from "@/lib/metrics/range"
import { USER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

const SIDEBAR_COLUMN_LABELS: Record<SidebarColumnId, string> = {
  cpu: "CPU",
  ram: "Memory",
}

function ProfileField({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="flex max-w-md flex-col gap-1.5">
      <p className="text-sm font-medium">{label}</p>
      {children}
      {hint ? (
        <p className="text-xs font-bold text-neutral-500">{hint}</p>
      ) : null}
    </div>
  )
}

function ReadOnlyField({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        "rounded-sm border border-border bg-background px-3 py-2 text-sm",
        className
      )}
    >
      {children}
    </div>
  )
}

type AccountSettingsViewProps = {
  user: User
}

function AccountSettingsView({ user }: AccountSettingsViewProps) {
  const { refreshInterval, setRefreshInterval } = useMetricRefreshInterval()
  const { defaultRange, setDefaultRange } = useMetricDefaultRange()
  const { detailed, setDetailed } = useSidebarDetailedMode()
  const { visibility, setColumnVisible } = useSidebarColumnVisibility()

  return (
    <SettingsPageContent>
      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Profile"
          description="Your account identity."
        />

        <div className="flex max-w-xl flex-col gap-4">
          <ProfileField
            label="Email"
            hint="Contact an administrator to change your email."
          >
            <Input value={user.email} readOnly disabled className="max-w-md" />
          </ProfileField>

          <ProfileField
            label="Role"
            hint="Contact an administrator to change your role."
          >
            <ReadOnlyField className="flex flex-col gap-2">
              <UserRoleBadge role={user.role} />
              <p className="text-xs leading-relaxed text-muted-foreground">
                {USER_ROLE_TOOLTIPS[user.role]}
              </p>
            </ReadOnlyField>
          </ProfileField>

          <ProfileField label="Member since">
            <ReadOnlyField>
              <SimpleTooltip content={formatDateWithRelative(user.createdAt)}>
                <span className="cursor-help text-muted-foreground">
                  {formatDate(user.createdAt)}
                </span>
              </SimpleTooltip>
            </ReadOnlyField>
          </ProfileField>
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Security"
          description="Update your sign-in password."
        />
        <ChangePasswordForm />
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Appearance"
          description="How Monitor looks on this device."
        />
        <SettingsPreferenceRow
          label="Theme"
          description="Light, dark, or match your system setting."
          control={<ThemeSwitcher />}
        />
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Metrics"
          description="Defaults for metric charts and dashboards."
        />
        <SettingsPreferenceRow
          label="Time frame"
          description="Preset range when opening a server's metrics."
          control={
            <Select
              value={defaultRange}
              onValueChange={(value) =>
                setDefaultRange(value as MetricTimeRange)
              }
            >
              <SelectTrigger className="w-[180px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {METRIC_RANGE_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          }
        />
        <SettingsPreferenceRow
          label="Refresh interval"
          description="How often open metric views poll for new data."
          control={
            <Select
              value={refreshInterval}
              onValueChange={(value) =>
                setRefreshInterval(value as MetricRefreshInterval)
              }
            >
              <SelectTrigger className="w-[180px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {METRIC_REFRESH_INTERVAL_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          }
        />
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Sidebar"
          description="Server list layout in the navigation panel."
        />

        <div className="flex flex-col gap-4">
          <SettingsPreferenceRow
            label="Detailed mode"
            description="Show live CPU and memory stats under each server name."
            control={
              <SettingsToggle
                checked={detailed}
                ariaLabel="Detailed mode"
                onCheckedChange={setDetailed}
              />
            }
          />

          {detailed
            ? SIDEBAR_COLUMNS.map((column) => (
                <SettingsPreferenceRow
                  key={column}
                  label={SIDEBAR_COLUMN_LABELS[column]}
                  description={`Show ${SIDEBAR_COLUMN_LABELS[column].toLowerCase()} in detailed server rows.`}
                  control={
                    <SettingsToggle
                      checked={visibility[column]}
                      ariaLabel={SIDEBAR_COLUMN_LABELS[column]}
                      onCheckedChange={(checked) =>
                        setColumnVisible(column, checked)
                      }
                    />
                  }
                />
              ))
            : null}
        </div>
      </section>
    </SettingsPageContent>
  )
}

export { AccountSettingsView }
