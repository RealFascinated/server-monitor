import { Server } from "lucide-react"

import { PageHeader } from "@/components/page-header"

function ServersHeader() {
  return (
    <PageHeader
      icon={Server}
      title="Servers"
      description="Health and metrics for servers you own or can view."
    />
  )
}

export { ServersHeader }
