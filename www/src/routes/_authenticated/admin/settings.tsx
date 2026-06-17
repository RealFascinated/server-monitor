import { useQuery } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"

import { AdminSettingsHeader } from "@/components/admin/admin-settings-header"
import { AdminSettingsView } from "@/components/admin/admin-settings-view"
import { QueryStatusShell } from "@/components/query-status-shell"
import { adminSettingsQueryOptions } from "@/lib/api/admin/settings.queries"
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

  return (
    <section className={authenticatedPageSectionClassName}>
      <AdminSettingsHeader />

      <QueryStatusShell
        error={error}
        isPending={isPending}
        loadingMessage="Loading settings…"
        fallbackMessage="Failed to load admin settings"
        fallbackTitle="Could not load settings"
      >
        {settings ? <AdminSettingsView settings={settings} /> : null}
      </QueryStatusShell>
    </section>
  )
}
