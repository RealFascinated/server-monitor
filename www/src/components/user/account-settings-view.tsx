import { TimestampCell } from "@/components/timestamp-cell"
import { SettingsPageContent } from "@/components/settings/settings-page-content"
import { SettingsPreferenceGroup } from "@/components/settings/settings-preference-group"
import { SettingsPreferenceRow } from "@/components/settings/settings-preference-row"
import { SettingsSectionHeader } from "@/components/settings/settings-section-header"
import { SettingsSubsectionHeader } from "@/components/settings/settings-subsection-header"
import { ThemeSwitcher } from "@/components/theme-switcher"
import { Card, CardContent } from "@/components/ui/card"
import { ChangePasswordForm } from "@/components/user/change-password-form"
import { UserSessionsSection } from "@/components/user/user-sessions-section"
import { UserRoleBadge } from "@/components/user/user-role-badge"
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
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { METRIC_REFRESH_INTERVAL_OPTIONS } from "@/lib/metrics/refresh-interval"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { METRIC_RANGE_OPTIONS } from "@/lib/metrics/range"
import { cn } from "@/lib/utils"

const SIDEBAR_COLUMN_LABELS: Record<SidebarColumnId, string> = {
  cpu: "CPU",
  ram: "Memory",
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
          description="Your account identity and membership."
        />

        <Card className="max-w-xl gap-0 py-0 shadow-none">
          <CardContent className="flex flex-col gap-3 py-4">
            <div className="flex flex-wrap items-center gap-2.5">
              <p className="text-base font-medium text-foreground">{user.email}</p>
              <UserRoleBadge role={user.role} />
            </div>
            <p className="text-sm text-muted-foreground">
              Member since{" "}
              <TimestampCell
                iso={user.createdAt}
                className="underline decoration-dotted underline-offset-[3px]"
              />
            </p>
          </CardContent>
        </Card>
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Security"
          description="Update your sign-in password and manage active sessions."
        />

        <div className="flex max-w-xl flex-col gap-6">
          <div className="flex flex-col gap-3">
            <SettingsSubsectionHeader
              title="Password"
              description="Choose a strong password you do not use elsewhere."
            />
            <div className="rounded-sm border border-border bg-card px-4 py-4">
              <ChangePasswordForm />
            </div>
          </div>

          <UserSessionsSection />
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Appearance"
          description="How Monitor looks on this device."
        />
        <SettingsPreferenceGroup>
          <SettingsPreferenceRow
            label="Theme"
            description="Light, dark, or match your system setting."
            control={<ThemeSwitcher />}
          />
        </SettingsPreferenceGroup>
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Metrics"
          description="Defaults for metric charts and dashboards."
        />
        <SettingsPreferenceGroup>
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
                <SelectTrigger className="w-full sm:w-[180px]">
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
                <SelectTrigger className="w-full sm:w-[180px]">
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
        </SettingsPreferenceGroup>
      </section>

      <section className="flex flex-col gap-4">
        <SettingsSectionHeader
          title="Sidebar"
          description="Server list layout in the navigation panel."
        />

        <SettingsPreferenceGroup>
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
        </SettingsPreferenceGroup>

        {detailed ? (
          <SettingsPreferenceGroup
            className={cn("ml-3 border-l-2 border-l-primary/20 sm:ml-4")}
          >
            {SIDEBAR_COLUMNS.map((column) => (
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
            ))}
          </SettingsPreferenceGroup>
        ) : null}
      </section>
    </SettingsPageContent>
  )
}

export { AccountSettingsView }
