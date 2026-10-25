import { createFileRoute, redirect } from "@tanstack/react-router"

export const Route = createFileRoute("/_authenticated/alerts/")({
  beforeLoad: () => {
    throw redirect({ to: "/alerts/groups", replace: true })
  },
})
