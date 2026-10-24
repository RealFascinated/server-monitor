import { memo, useState } from "react"

import { SidebarFolderList } from "@/components/sidebar/sidebar-folder-list"
import { useUserServers } from "@/hooks/use-user-servers"
import { SidebarSearch } from "@/components/sidebar/sidebar-search"

const SIDEBAR_SEARCH_MIN_SERVERS = 8

export const SidebarFolderTree = memo(function SidebarFolderTree({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const [searchQuery, setSearchQuery] = useState("")
  const { data: servers = [] } = useUserServers()
  const showSearchInput = servers.length > SIDEBAR_SEARCH_MIN_SERVERS

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {!compact ? (
        <SidebarSearch
          value={searchQuery}
          onChange={setSearchQuery}
          showInput={showSearchInput}
        />
      ) : (
        <div className="my-2 shrink-0 border-t border-sidebar-border" />
      )}
      <SidebarFolderList
        compact={compact}
        searchQuery={showSearchInput ? searchQuery : ""}
        onNavigate={onNavigate}
      />
    </div>
  )
})
