import { Mail } from "lucide-react"

import { PageHeader } from "@/components/page-header"

function InvitesHeader() {
  return (
    <PageHeader
      icon={Mail}
      title="Invites"
      description="Server invites sent to your email address."
    />
  )
}

export { InvitesHeader }
