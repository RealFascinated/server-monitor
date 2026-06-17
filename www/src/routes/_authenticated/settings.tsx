import { createFileRoute } from "@tanstack/react-router"

import { AsyncContent } from "@/components/animated-content"
import { AccountSettingsHeader } from "@/components/user/account-settings-header"
import { AccountSettingsView } from "@/components/user/account-settings-view"
import { useAuth } from "@/lib/auth"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/settings")({
  head: () => ({
    meta: [{ title: pageTitle("Account") }],
  }),
  component: AccountSettingsPage,
})

function AccountSettingsPage() {
  const { user } = useAuth()

  return (
    <section className={authenticatedPageSectionClassName}>
      <AccountSettingsHeader email={user?.email} />
      <AsyncContent loading={!user} loadingMessage="Loading account…">
        {user ? <AccountSettingsView user={user} /> : null}
      </AsyncContent>
    </section>
  )
}
