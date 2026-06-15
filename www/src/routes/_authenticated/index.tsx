import { createFileRoute } from "@tanstack/react-router"

import { ServersHeader } from "@/components/user/servers-header"
import { ServersTable } from "@/components/user/servers-table"
import { authenticatedPageSectionClassName } from "@/lib/layout"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/")({
  head: () => ({
    meta: [{ title: pageTitle("Servers") }],
  }),
  component: ServersPage,
})

function ServersPage() {
  return (
    <section className={authenticatedPageSectionClassName}>
      <ServersHeader />
      <ServersTable />
    </section>
  )
}
