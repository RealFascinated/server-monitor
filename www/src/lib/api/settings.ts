import {
  getAdminSettingValue,
  normalizeAdminSettings,
} from "@/lib/api/admin/settings"
import type { AdminSettingResponse } from "@/lib/api/admin/settings"
import { apiFetch } from "@/lib/auth/api"
import { Settings } from "@/lib/settings"

export type PublicSettingResponse = AdminSettingResponse

export function getPublicSettings(): Promise<PublicSettingResponse[]> {
  return apiFetch<unknown>("/v1/settings", { auth: false }).then(
    normalizeAdminSettings
  )
}

export function isRegistrationEnabled(
  settings: PublicSettingResponse[]
): boolean {
  return getAdminSettingValue(settings, Settings.REGISTRATION_ENABLED)
}
