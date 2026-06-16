import { apiFetch } from "@/lib/auth/api"

export type UserPreferenceResponse = {
  key: string
  value: boolean | string | number
  lastModified: string | null
}

export function getUserPreferences(): Promise<UserPreferenceResponse[]> {
  return apiFetch<UserPreferenceResponse[]>("/v1/user/preferences")
}

export function updateUserPreference(
  key: string,
  value: boolean | string | number
): Promise<UserPreferenceResponse> {
  return apiFetch<UserPreferenceResponse>(`/v1/user/preferences/${key}`, {
    method: "PUT",
    body: JSON.stringify({ value }),
  })
}
