import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useQuery } from "@tanstack/react-query"
import { LogIn, UserRoundX } from "lucide-react"
import { useEffect } from "react"

import { AuthPageFooter, AuthPageLink } from "@/components/auth-page-footer"
import { Callout } from "@/components/callout"
import { LoadingState } from "@/components/loading-state"
import { AuthForm } from "@/components/auth-form"
import { AuthPageShell } from "@/components/auth-page-shell"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { isRegistrationEnabled } from "@/lib/api/settings"
import { getApiErrorMessage, getApiErrorTitle } from "@/lib/api/error-message"
import { publicSettingsQueryOptions } from "@/lib/api/settings.queries"
import { useAuth } from "@/lib/auth"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/register")({
  ssr: false,
  head: () => ({
    meta: [{ title: pageTitle("Create account") }],
  }),
  loader: ({ context: { queryClient } }) => {
    return queryClient.ensureQueryData(publicSettingsQueryOptions())
  },
  component: RegisterPage,
})

function RegisterPage() {
  const { user, isLoading } = useAuth()
  const navigate = useNavigate()
  const {
    data: settings,
    isPending: settingsPending,
    isError: settingsError,
    error: settingsLoadError,
  } = useQuery(publicSettingsQueryOptions())

  useEffect(() => {
    if (!isLoading && user) {
      void navigate({ to: "/" })
    }
  }, [isLoading, user, navigate])

  if (isLoading || settingsPending) {
    return <LoadingState message="Checking session…" centered />
  }

  if (settingsError) {
    const errorTitle = settingsLoadError
      ? getApiErrorTitle(settingsLoadError, "Could not load registration status")
      : "Could not load registration status"
    const errorMessage = settingsLoadError
      ? getApiErrorMessage(
          settingsLoadError,
          "Try refreshing the page. If the problem continues, contact an administrator."
        )
      : "Try refreshing the page. If the problem continues, contact an administrator."

    return (
      <AuthPageShell>
        <Card className="motion-auth-card w-full max-w-md">
          <CardContent className="pt-6">
            <Callout type="danger" title={errorTitle}>
              {errorMessage}
            </Callout>
          </CardContent>
        </Card>
      </AuthPageShell>
    )
  }

  if (!isRegistrationEnabled(settings)) {
    return (
      <AuthPageShell>
        <Card className="motion-auth-card w-full max-w-md">
          <CardHeader className="items-center text-center">
            <div className="mb-1 flex size-12 items-center justify-center rounded-full bg-muted">
              <UserRoundX className="size-6 text-muted-foreground" />
            </div>
            <CardTitle>Registration is closed</CardTitle>
            <CardDescription className="text-balance">
              This instance isn&apos;t accepting new accounts. Sign in if you
              already have one, or contact an administrator for access.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild variant="highlighted" className="w-full">
              <Link to="/login">
                <LogIn />
                Sign in
              </Link>
            </Button>
          </CardContent>
        </Card>
      </AuthPageShell>
    )
  }

  return (
    <AuthPageShell>
      <Card className="motion-auth-card w-full max-w-md">
        <CardHeader>
          <CardTitle>Create account</CardTitle>
          <CardDescription>
            The first account becomes the administrator.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <AuthForm mode="register" />
          <AuthPageFooter>
            Already have an account?{" "}
            <AuthPageLink to="/login">Sign in</AuthPageLink>
          </AuthPageFooter>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
