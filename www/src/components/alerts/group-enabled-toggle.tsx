import { SettingsToggle } from "@/components/ui/settings-toggle"
import { useUpdateAlertGroup } from "@/hooks/use-alert-groups"
import type { AlertGroup } from "@/lib/api/user/alerts"

/** Switches a whole group on or off; a disabled group evaluates none of its alerts. */
function GroupEnabledToggle({ group }: { group: AlertGroup }) {
  const update = useUpdateAlertGroup()

  return (
    <SettingsToggle
      ariaLabel={`${group.enabled ? "Disable" : "Enable"} ${group.name}`}
      checked={group.enabled}
      disabled={update.isPending}
      onCheckedChange={(enabled) =>
        update.mutate({ id: group.id, request: { enabled } })
      }
    />
  )
}

export { GroupEnabledToggle }
