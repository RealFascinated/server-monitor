import { createFileRoute } from "@tanstack/react-router"

import { AlertDestinationsView } from "@/components/alerts/alert-destinations-view"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/alerts/destinations")({
  head: () => ({
    meta: [{ title: pageTitle("Alert destinations") }],
  }),
  component: AlertDestinationsView,
})
