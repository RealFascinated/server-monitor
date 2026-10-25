import { Link } from "@tanstack/react-router"
import { FolderKanban, History, Webhook } from "lucide-react"
import type { LucideIcon } from "lucide-react"

const tabClassName =
  "-mb-px flex shrink-0 items-center gap-1.5 border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none [&.active]:border-primary [&.active]:text-foreground"

function TabIcon({ icon: Icon }: { icon: LucideIcon }) {
  return <Icon aria-hidden className="size-3.5" />
}

/** Section navigation shared by every page under /alerts. */
function AlertsTabs() {
  return (
    <nav
      aria-label="Alert sections"
      className="-mx-1 -mb-3 flex overflow-x-auto overflow-y-hidden px-1"
    >
      <Link to="/alerts/groups" className={tabClassName}>
        <TabIcon icon={FolderKanban} />
        Groups
      </Link>
      <Link to="/alerts/destinations" className={tabClassName}>
        <TabIcon icon={Webhook} />
        Destinations
      </Link>
      <Link to="/alerts/history" className={tabClassName}>
        <TabIcon icon={History} />
        History
      </Link>
    </nav>
  )
}

export { AlertsTabs }
