import type { SettingTypeName } from "@/lib/settings"
import { apiFetch } from "@/lib/auth/api"

export type AdminSettingResponse = {
  key: string
  type: SettingTypeName
  value: boolean | string | number
  lastModified: string | null
}

export type AdminSettingUpdateRequest = {
  value: boolean | string | number
}

function isAdminSettingResponse(value: unknown): value is AdminSettingResponse {
  return (
    typeof value === "object" &&
    value !== null &&
    "key" in value &&
    "type" in value &&
    "value" in value
  )
}

export function normalizeAdminSettings(data: unknown): AdminSettingResponse[] {
  if (Array.isArray(data)) {
    return data
  }

  if (isAdminSettingResponse(data)) {
    return [data]
  }

  if (data && typeof data === "object") {
    const values = Object.values(data)
    if (values.every(isAdminSettingResponse)) {
      return values
    }
  }

  return []
}

export function getAdminSettingValue<T extends boolean | string | number>(
  settings: AdminSettingResponse[],
  definition: { key: string; defaultValue: T }
): T {
  const setting = settings.find((entry) => entry.key === definition.key)
  return (setting?.value as T | undefined) ?? definition.defaultValue
}

export function mergeAdminSettingsCache(
  current: AdminSettingResponse[] | undefined,
  updated: AdminSettingResponse[]
): AdminSettingResponse[] {
  const existing = Array.isArray(current) ? current : []

  if (updated.length === 1 && existing.length > 0) {
    const next = updated[0]
    const index = existing.findIndex((entry) => entry.key === next.key)
    if (index === -1) {
      return [...existing, next]
    }
    return existing.map((entry, i) => (i === index ? next : entry))
  }

  return updated.length > 0 ? updated : existing
}

export function getAdminSettings(): Promise<AdminSettingResponse[]> {
  return apiFetch<unknown>("/v1/admin/settings").then(normalizeAdminSettings)
}

export function updateAdminSetting(
  key: string,
  request: AdminSettingUpdateRequest
): Promise<AdminSettingResponse[]> {
  return apiFetch<unknown>(`/v1/admin/settings/${key}`, {
    method: "PUT",
    body: JSON.stringify(request),
  }).then(normalizeAdminSettings)
}
