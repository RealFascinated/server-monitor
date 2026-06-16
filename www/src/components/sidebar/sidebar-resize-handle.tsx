import type { PointerEvent as ReactPointerEvent } from "react"

export function SidebarResizeHandle({
  onResizeStart,
}: {
  onResizeStart: (event: ReactPointerEvent<HTMLDivElement>) => void
}) {
  return (
    <div
      role="separator"
      aria-orientation="vertical"
      aria-label="Resize sidebar"
      onPointerDown={onResizeStart}
      className="absolute top-0 right-0 z-20 hidden h-full w-1 cursor-col-resize touch-none hover:bg-muted-foreground/20 active:bg-muted-foreground/30 lg:block"
    />
  )
}
