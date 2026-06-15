import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

type SettingsPageContentProps = {
  children: ReactNode
  className?: string
}

function SettingsPageContent({
  children,
  className,
}: SettingsPageContentProps) {
  return <div className={cn("flex flex-col gap-8", className)}>{children}</div>
}

export { SettingsPageContent }
