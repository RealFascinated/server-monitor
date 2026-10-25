import { createFileRoute } from "@tanstack/react-router"

import { AlertGroupView } from "@/components/alerts/alert-group-view"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/_authenticated/alerts/groups/$groupId/")(
  {
    head: () => ({
      meta: [{ title: pageTitle("Alert group") }],
    }),
    component: AlertGroupRoute,
  }
)

function AlertGroupRoute() {
  const { groupId } = Route.useParams()
  return <AlertGroupView groupId={Number(groupId)} />
}
