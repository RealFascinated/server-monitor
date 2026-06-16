import { createFileRoute } from "@tanstack/react-router"
import { z } from "zod"

import { AuthPageFooter, AuthPageLink } from "@/components/auth-page-footer"
import { Callout } from "@/components/callout"
import { AuthPageShell } from "@/components/auth-page-shell"
import { ResetPasswordForm } from "@/components/reset-password-form"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { pageTitle } from "@/lib/page-title"

const resetPasswordSearchSchema = z.object({
  token: z.string().min(1).optional(),
})

export const Route = createFileRoute("/reset-password")({
  ssr: false,
  validateSearch: resetPasswordSearchSchema,
  head: () => ({
    meta: [{ title: pageTitle("Reset password") }],
  }),
  component: ResetPasswordPage,
})

function ResetPasswordPage() {
  const { token } = Route.useSearch()

  return (
    <AuthPageShell>
      <Card className="motion-auth-card w-full max-w-md">
        <CardHeader>
          <CardTitle>Reset password</CardTitle>
          <CardDescription>Choose a new password for your account.</CardDescription>
        </CardHeader>
        <CardContent>
          {token ? (
            <ResetPasswordForm token={token} />
          ) : (
            <Callout type="danger" title="Invalid reset link">
              This reset link is missing a token. Request a new link from the
              forgot password page.
            </Callout>
          )}
          <AuthPageFooter>
            Need a new link?{" "}
            <AuthPageLink to="/forgot-password">Request reset link</AuthPageLink>
          </AuthPageFooter>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
