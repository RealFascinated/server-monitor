import { createFileRoute } from "@tanstack/react-router"
import { z } from "zod"

import { Callout } from "@/components/callout"
import { AcceptInviteView } from "@/components/user/accept-invite-view"
import { pageTitle } from "@/lib/page-title"

const acceptInviteSearchSchema = z.object({
  token: z.string().min(1),
  email: z.string().optional(),
})

export const Route = createFileRoute("/_authenticated/invites/accept")({
  validateSearch: acceptInviteSearchSchema,
  head: () => ({
    meta: [{ title: pageTitle("Accept invite") }],
  }),
  component: AcceptInvitePage,
})

function AcceptInvitePage() {
  const { token } = Route.useSearch()

  return (
    <section className="flex flex-col gap-6 py-8">
      {token ? (
        <AcceptInviteView token={token} />
      ) : (
        <Callout type="danger" title="Invalid invite link">
          This invite link is missing a token. Ask the server owner to send a
          new invite.
        </Callout>
      )}
    </section>
  )
}
