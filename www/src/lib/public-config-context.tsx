import { createContext, useContext, useMemo } from "react"

import type { PublicConfig } from "@/env/public-config"
import { setPublicConfig } from "@/lib/public-config"

const PublicConfigContext = createContext<PublicConfig | null>(null)

function PublicConfigProvider({
  config,
  children,
}: {
  config: PublicConfig
  children: React.ReactNode
}) {
  setPublicConfig(config)

  const value = useMemo(() => config, [config])

  return (
    <PublicConfigContext.Provider value={value}>
      {children}
    </PublicConfigContext.Provider>
  )
}

function usePublicConfig(): PublicConfig {
  const config = useContext(PublicConfigContext)
  if (!config) {
    throw new Error("usePublicConfig must be used within PublicConfigProvider")
  }
  return config
}

export { PublicConfigProvider, usePublicConfig }
