import { Outlet, createFileRoute } from "@tanstack/react-router"
import { Bell } from "lucide-react"

import { AlertsTabs } from "@/components/alerts/alerts-tabs"
import { PageHeader } from "@/components/page-header"
import { authenticatedPageSectionClassName } from "@/lib/layout"

export const Route = createFileRoute("/_authenticated/alerts")({
  component: AlertsLayout,
})

function AlertsLayout() {
  return (
    <section className={authenticatedPageSectionClassName}>
      <PageHeader
        icon={Bell}
        title="Alerts"
        description="Get notified when a metric crosses a threshold or a server goes offline."
        footer={<AlertsTabs />}
      />
      <Outlet />
    </section>
  )
}
