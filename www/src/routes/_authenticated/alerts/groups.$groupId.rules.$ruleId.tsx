import { createFileRoute } from "@tanstack/react-router"

import { AlertBuilderView } from "@/components/alerts/alert-builder-view"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute(
  "/_authenticated/alerts/groups/$groupId/rules/$ruleId"
)({
  head: () => ({
    meta: [{ title: pageTitle("Edit alert") }],
  }),
  component: EditAlertRoute,
})

function EditAlertRoute() {
  const { groupId, ruleId } = Route.useParams()
  return <AlertBuilderView groupId={Number(groupId)} ruleId={Number(ruleId)} />
}
