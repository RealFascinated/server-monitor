import { Outlet, createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_authenticated/invites")({
  component: InvitesLayout,
})

function InvitesLayout() {
  return <Outlet />
}
