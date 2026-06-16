import { useQuery } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"

import { AdminSettingsHeader } from "@/components/admin/admin-settings-header"
import { AdminSettingsView } from "@/components/admin/admin-settings-view"
import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { adminSettingsQueryOptions } from "@/lib/api/admin/settings.queries"
import { getApiErrorMessage, getApiErrorTitle } from "@/lib/api/error-message"
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

  const errorMessage = error
    ? getApiErrorMessage(error, "Failed to load admin settings")
    : null
  const errorTitle = error
    ? getApiErrorTitle(error, "Could not load settings")
    : null

  return (
    <section className={authenticatedPageSectionClassName}>
      <AdminSettingsHeader />

      {errorMessage ? (
        <Callout type="danger" title={errorTitle ?? "Could not load settings"}>
          {errorMessage}
        </Callout>
      ) : null}

      <AsyncContent
        loading={isPending && !errorMessage}
        loadingMessage="Loading settings…"
      >
        {settings && !errorMessage ? (
          <AdminSettingsView settings={settings} />
        ) : null}
      </AsyncContent>
    </section>
  )
}
