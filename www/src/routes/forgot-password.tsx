import { createFileRoute } from "@tanstack/react-router"

import { AuthPageFooter, AuthPageLink } from "@/components/auth-page-footer"
import { AuthPageShell } from "@/components/auth-page-shell"
import { ForgotPasswordForm } from "@/components/forgot-password-form"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { pageTitle } from "@/lib/page-title"

export const Route = createFileRoute("/forgot-password")({
  ssr: false,
  head: () => ({
    meta: [{ title: pageTitle("Forgot password") }],
  }),
  component: ForgotPasswordPage,
})

function ForgotPasswordPage() {
  return (
    <AuthPageShell>
      <Card className="motion-auth-card w-full max-w-md">
        <CardHeader>
          <CardTitle>Forgot password</CardTitle>
          <CardDescription>
            Enter your email and we&apos;ll send a reset link if an account
            exists.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ForgotPasswordForm />
          <AuthPageFooter>
            Remember your password?{" "}
            <AuthPageLink to="/login">Sign in</AuthPageLink>
          </AuthPageFooter>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
