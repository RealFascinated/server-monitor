import { Outlet, createFileRoute } from "@tanstack/react-router"

import { requireAdmin } from "@/lib/auth/require-admin"

export const Route = createFileRoute("/_authenticated/admin")({
  ssr: false,
  beforeLoad: () => requireAdmin(),
  component: AdminLayout,
})

function AdminLayout() {
  return <Outlet />
}
