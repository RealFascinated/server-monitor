import { useQuery } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"

import { AdminSettingsHeader } from "@/components/admin/admin-settings-header"
import { AdminSettingsView } from "@/components/admin/admin-settings-view"
import { AnimatedContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { LoadingState } from "@/components/loading-state"
import { adminSettingsQueryOptions } from "@/lib/api/admin/settings.queries"
import { ApiClientError } from "@/lib/auth/api"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/admin/settings")({
  ssr: false,
  loader: ({ context: { queryClient } }) => {
    return queryClient.ensureQueryData(adminSettingsQueryOptions())
  },
  head: () => ({
    meta: [{ title: pageTitle("Admin Settings") }],
  }),
  component: AdminSettingsPage,
})

function AdminSettingsPage() {
  const {
    data: settings,
    isPending,
    error,
  } = useQuery(adminSettingsQueryOptions())

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : error
        ? "Failed to load admin settings"
        : null

  return (
    <section className={authenticatedPageSectionClassName}>
      <AdminSettingsHeader />

      {errorMessage ? (
        <Callout type="danger" title="Could not load settings">
          {errorMessage}
        </Callout>
      ) : null}

      {isPending && !errorMessage ? (
        <LoadingState message="Loading settings…" />
      ) : null}

      {settings && !errorMessage ? (
        <AnimatedContent>
          <AdminSettingsView settings={settings} />
        </AnimatedContent>
      ) : null}
    </section>
  )
}
