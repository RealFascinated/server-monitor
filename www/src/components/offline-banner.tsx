import { WifiOff } from "lucide-react"

import { useOnlineStatus } from "@/hooks/use-online-status"
import { cn } from "@/lib/utils"

type OfflineBannerProps = {
  className?: string
}

function OfflineBanner({ className }: OfflineBannerProps) {
  const isOnline = useOnlineStatus()

  if (isOnline) {
    return null
  }

  return (
    <div
      role="status"
      className={cn(
        "flex items-center justify-center gap-2 border-b border-warning-300 bg-warning-50 px-4 py-2 text-sm font-medium text-warning-800 dark:border-warning-800 dark:bg-warning-900/30 dark:text-warning-300",
        className
      )}
    >
      <WifiOff className="size-4 shrink-0" aria-hidden />
      You're offline. Some features may not work until your connection returns.
    </div>
  )
}

export { OfflineBanner }
