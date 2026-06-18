import { z } from "zod"

export const publicConfigSchema = z.looseObject({
  apiUrl: z.string().min(1),
})

export type PublicConfig = z.infer<typeof publicConfigSchema>

const PUBLIC_ENV_PREFIX = "MONITOR_PUBLIC_"

function envSuffixToConfigKey(suffix: string): string {
  return suffix
    .toLowerCase()
    .replace(/_([a-z])/g, (_, char: string) => char.toUpperCase())
}

function trimTrailingSlash(url: string): string {
  return url.replace(/\/$/, "")
}

function readPrefixedEnvVars(): Record<string, string> {
  const values: Record<string, string> = {}

  if (typeof process === "undefined") {
    return values
  }

  for (const [key, value] of Object.entries(process.env)) {
    if (!value || !key.startsWith(PUBLIC_ENV_PREFIX)) {
      continue
    }

    const suffix = key.slice(PUBLIC_ENV_PREFIX.length)
    values[envSuffixToConfigKey(suffix)] = value
  }

  return values
}

/** Load public runtime config from the server environment. */
export function loadPublicConfigFromEnv(): PublicConfig {
  const values = readPrefixedEnvVars()

  if (values.apiUrl) {
    values.apiUrl = trimTrailingSlash(values.apiUrl)
  }

  return publicConfigSchema.parse(values)
}
