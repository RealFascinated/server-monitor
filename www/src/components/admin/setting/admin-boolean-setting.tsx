import { SimpleTooltip } from "@/components/simple-tooltip"
import { SettingsPreferenceRow } from "@/components/settings/settings-preference-row"
import { SettingsToggle } from "@/components/ui/settings-toggle"
import { useAdminSettingUpdate } from "@/hooks/use-admin-setting-update"
import type { AdminSettingResponse } from "@/lib/api/admin/settings"
import { getAdminSettingValue } from "@/lib/api/admin/settings"
import type { BooleanSettingDefinition } from "@/lib/settings"

type AdminBooleanSettingProps = {
  settings: AdminSettingResponse[]
  definition: BooleanSettingDefinition
  label: string
  description: string
  tooltip?: string
}

function AdminBooleanSetting({
  settings,
  definition,
  label,
  description,
  tooltip,
}: AdminBooleanSettingProps) {
  const { mutation } = useAdminSettingUpdate(definition)
  const checked = getAdminSettingValue(settings, definition)

  const control = (
    <SettingsToggle
      checked={checked}
      disabled={mutation.isPending}
      ariaLabel={label}
      onCheckedChange={(next) => mutation.mutate(next)}
    />
  )

  return (
    <SettingsPreferenceRow
      label={label}
      description={description}
      control={
        tooltip ? (
          <SimpleTooltip content={tooltip}>{control}</SimpleTooltip>
        ) : (
          control
        )
      }
    />
  )
}

export { AdminBooleanSetting }
