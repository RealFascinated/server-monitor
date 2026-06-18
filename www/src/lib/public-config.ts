import type { PublicConfig } from "@/env/public-config"
import { loadPublicConfigFromEnv } from "@/env/public-config"

let activeConfig: PublicConfig | null = null

export function setPublicConfig(config: PublicConfig): void {
  activeConfig = config
}

export function getPublicConfig(): PublicConfig {
  if (activeConfig) {
    return activeConfig
  }

  if (typeof process !== "undefined") {
    return loadPublicConfigFromEnv()
  }

  throw new Error("Public config is not available")
}

export function getApiBaseUrl(): string {
  return getPublicConfig().apiUrl
}
