import { createFileRoute } from "@tanstack/react-router"

import { InvitesHeader } from "@/components/user/invites-header"
import { UserPendingInvites } from "@/components/user/user-pending-invites"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/invites/")({
  head: () => ({
    meta: [{ title: pageTitle("Invites") }],
  }),
  component: InvitesPage,
})

function InvitesPage() {
  return (
    <section className={authenticatedPageSectionClassName}>
      <InvitesHeader />
      <UserPendingInvites />
    </section>
  )
}
