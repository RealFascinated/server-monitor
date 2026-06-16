import type { CSSProperties, ReactNode } from "react"

import { cn } from "@/lib/utils"

export const MOBILE_SIDEBAR_WIDTH = 280

export function MobileSidebarDrawer({
  mobileOpen,
  onMobileClose,
  width,
  isResizing,
  children,
}: {
  mobileOpen: boolean
  onMobileClose: () => void
  width: number
  isResizing: boolean
  children: ReactNode
}) {
  return (
    <>
      <button
        type="button"
        aria-label="Close sidebar"
        aria-hidden={!mobileOpen}
        tabIndex={mobileOpen ? 0 : -1}
        onClick={onMobileClose}
        className={cn(
          "fixed inset-0 z-40 bg-black/50 transition-opacity duration-150 ease-out lg:hidden",
          mobileOpen
            ? "pointer-events-auto opacity-100"
            : "pointer-events-none opacity-0"
        )}
      />

      <aside
        style={
          {
            "--sidebar-inline-width": `${width}px`,
            "--mobile-sidebar-width": `${MOBILE_SIDEBAR_WIDTH}px`,
          } as CSSProperties
        }
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-[min(var(--mobile-sidebar-width),85vw)] flex-col border-r border-sidebar-border bg-sidebar lg:w-[length:var(--sidebar-inline-width)]",
          mobileOpen ? "translate-x-0" : "-translate-x-full",
          "lg:translate-x-0",
          !mobileOpen && "pointer-events-none lg:pointer-events-auto",
          "transition-transform duration-150 ease-out lg:duration-200 lg:ease-in-out",
          !isResizing && "lg:transition-[width,transform]"
        )}
      >
        {children}
      </aside>
    </>
  )
}
