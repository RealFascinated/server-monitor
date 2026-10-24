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
  return (
    <div
      className={cn(
        "flex flex-col gap-6 [&>section+section]:border-t [&>section+section]:border-border/60 [&>section+section]:pt-6",
        className
      )}
    >
      {children}
    </div>
  )
}

export { SettingsPageContent }
