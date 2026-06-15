import type { ServerResponse } from "@/lib/api/user/servers"

export const APP_NAME = "Server Monitor"

export function pageTitle(...segments: (string | undefined)[]) {
  const parts = segments.filter(Boolean)
  if (parts.length === 0) {
    return APP_NAME
  }

  return [...parts, APP_NAME].join(" · ")
}

export function serverPageTitle(
  server: ServerResponse | undefined,
  ...extra: string[]
) {
  return pageTitle(...extra, server?.serverName)
}
