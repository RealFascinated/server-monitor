import { createFileRoute } from "@tanstack/react-router"

import { AlertGroupsView } from "@/components/alerts/alert-groups-view"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/alerts/groups/")({
  head: () => ({
    meta: [{ title: pageTitle("Alert groups") }],
  }),
  component: AlertGroupsView,
})
