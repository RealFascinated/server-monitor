import { AdminBooleanSetting } from "@/components/admin/setting/admin-boolean-setting"
import { SettingsPageContent } from "@/components/settings/settings-page-content"
import { SettingsSectionHeader } from "@/components/settings/settings-section-header"
import type { AdminSettingResponse } from "@/lib/api/admin/settings"
import { Settings } from "@/lib/settings"
import { SETTINGS_TOOLTIPS } from "@/lib/tooltips/copy"

type AdminSettingsViewProps = {
  settings: AdminSettingResponse[]
}

function AdminSettingsView({ settings }: AdminSettingsViewProps) {
  return (
    <SettingsPageContent>
      <section className="flex flex-col gap-3">
        <SettingsSectionHeader
          title="Authentication"
          description="Control who can create new accounts."
        />
        <AdminBooleanSetting
          settings={settings}
          definition={Settings.REGISTRATION_ENABLED}
          label="Allow registration"
          description="When disabled, only existing users can sign in."
          tooltip={SETTINGS_TOOLTIPS.registrationEnabled}
        />
      </section>
    </SettingsPageContent>
  )
}

export { AdminSettingsView }
