import { createFileRoute, redirect } from "@tanstack/react-router"

export const Route = createFileRoute("/_authenticated/admin/")({
  ssr: false,
  beforeLoad: () => {
    throw redirect({ to: "/admin/settings" })
  },
})
