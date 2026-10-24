import { Search } from "lucide-react"

import { openCommandPalette } from "@/components/command-palette"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { cn } from "@/lib/utils"

function isMacPlatform(): boolean {
  return (
    typeof navigator !== "undefined" &&
    /mac|iphone|ipad/i.test(navigator.platform)
  )
}

function SidebarQuickFind({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const shortcut = isMacPlatform() ? "⌘K" : "Ctrl K"

  const button = (
    <button
      type="button"
      aria-label={compact ? "Quick find" : undefined}
      aria-keyshortcuts="Control+K Meta+K"
      onClick={() => {
        onNavigate?.()
        openCommandPalette()
      }}
      className={cn(
        "mb-1 flex min-h-8 w-full items-center gap-3 rounded-md border border-border bg-card px-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none",
        compact && "justify-center px-0"
      )}
    >
      <Search aria-hidden className="size-4 shrink-0" />
      {!compact ? (
        <>
          <span className="flex-1 truncate text-left">Quick find</span>
          <kbd className="rounded-md border border-border px-1.5 font-sans text-[11px]">
            {shortcut}
          </kbd>
        </>
      ) : null}
    </button>
  )

  if (compact) {
    return (
      <SimpleTooltip content={`Quick find (${shortcut})`}>
        {button}
      </SimpleTooltip>
    )
  }

  return button
}

export { SidebarQuickFind }
