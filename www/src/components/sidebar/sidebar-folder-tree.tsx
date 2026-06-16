import { memo, useState } from "react"

import { SidebarFolderList } from "@/components/sidebar/sidebar-folder-list"
import { SidebarSearch } from "@/components/sidebar/sidebar-search"

export const SidebarFolderTree = memo(function SidebarFolderTree({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const [searchQuery, setSearchQuery] = useState("")

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {!compact ? (
        <SidebarSearch value={searchQuery} onChange={setSearchQuery} />
      ) : (
        <div className="my-2 shrink-0 border-t border-sidebar-border" />
      )}
      <SidebarFolderList
        compact={compact}
        searchQuery={searchQuery}
        onNavigate={onNavigate}
      />
    </div>
  )
})
