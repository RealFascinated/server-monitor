import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useQuery } from "@tanstack/react-query"
import { useEffect } from "react"

import { LoadingState } from "@/components/loading-state"
import { AuthForm } from "@/components/auth-form"
import { AuthPageShell } from "@/components/auth-page-shell"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { isRegistrationEnabled } from "@/lib/api/settings"
import { publicSettingsQueryOptions } from "@/lib/api/settings.queries"
import { useAuth } from "@/lib/auth"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/login")({
  ssr: false,
  head: () => ({
    meta: [{ title: pageTitle("Sign in") }],
  }),
  loader: ({ context: { queryClient } }) => {
    return queryClient.ensureQueryData(publicSettingsQueryOptions())
  },
  component: LoginPage,
})

function LoginPage() {
  const { user, isLoading } = useAuth()
  const navigate = useNavigate()
  const { data: settings } = useQuery(publicSettingsQueryOptions())
  const registrationEnabled = settings
    ? isRegistrationEnabled(settings)
    : undefined

  useEffect(() => {
    if (!isLoading && user) {
      void navigate({ to: "/" })
    }
  }, [isLoading, user, navigate])

  if (isLoading) {
    return <LoadingState message="Checking session…" centered />
  }

  return (
    <AuthPageShell>
      <Card className="motion-auth-card w-full max-w-md">
        <CardHeader>
          <CardTitle>Sign in</CardTitle>
          <CardDescription>
            Sign in to manage your monitored servers.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <AuthForm mode="login" />
          {registrationEnabled ? (
            <p className="pt-4 text-sm text-muted-foreground">
              No account?{" "}
              <Link
                to="/register"
                className="font-medium text-monitor dark:text-warning"
              >
                Create one
              </Link>
            </p>
          ) : null}
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
