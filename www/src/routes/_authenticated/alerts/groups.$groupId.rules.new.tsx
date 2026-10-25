import { createFileRoute } from "@tanstack/react-router"
import { z } from "zod"

import { AlertBuilderView } from "@/components/alerts/alert-builder-view"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute(
  "/_authenticated/alerts/groups/$groupId/rules/new"
)({
  validateSearch: z.object({ metric: z.string().optional() }),
  head: () => ({
    meta: [{ title: pageTitle("New alert") }],
  }),
  component: NewAlertRoute,
})

function NewAlertRoute() {
  const { groupId } = Route.useParams()
  const { metric } = Route.useSearch()
  return <AlertBuilderView groupId={Number(groupId)} initialMetric={metric} />
}
