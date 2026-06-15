import { SimpleTooltip } from "@/components/simple-tooltip"
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
    <div className="flex max-w-xl items-start justify-between gap-8">
      <div className="flex flex-col gap-1.5">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs font-bold text-neutral-500">{description}</p>
      </div>

      <div className="shrink-0 pt-0.5">
        {tooltip ? (
          <SimpleTooltip content={tooltip}>{control}</SimpleTooltip>
        ) : (
          control
        )}
      </div>
    </div>
  )
}

export { AdminBooleanSetting }
