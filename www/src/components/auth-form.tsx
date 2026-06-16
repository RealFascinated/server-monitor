import { useState } from "react"
import { Link, useNavigate } from "@tanstack/react-router"

import { AuthFormField, AuthFormShell } from "@/components/auth-form-shell"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuthForm } from "@/hooks/use-auth-form"
import { login, register, useAuth, validateCredentials } from "@/lib/auth"
import { isNetworkError } from "@/lib/network"

type AuthFormProps = {
  mode: "login" | "register"
}

function AuthForm({ mode }: AuthFormProps) {
  const navigate = useNavigate()
  const { setUser } = useAuth()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [apiErrorTitle, setApiErrorTitle] = useState<string | undefined>()
  const {
    fieldErrors,
    setFieldErrors,
    apiError,
    setApiError,
    isPending,
    setIsPending,
    clearErrors,
  } = useAuthForm<"email" | "password">()

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearErrors()
    setApiErrorTitle(undefined)

    try {
      const credentials = validateCredentials(
        { email, password },
        { minPasswordLength: mode === "register" ? 8 : 1 }
      )

      setIsPending(true)
      const result =
        mode === "login"
          ? await login(credentials)
          : await register(credentials)

      if ("error" in result) {
        setApiError(result.error)
        return
      }

      setUser(result.user)
      await navigate({ to: "/" })
    } catch (error) {
      if (error instanceof Error) {
        if (error.message.includes("email")) {
          setFieldErrors({ email: error.message })
        } else if (error.message.includes("Password")) {
          setFieldErrors({ password: error.message })
        } else {
          if (isNetworkError(error)) {
            setApiErrorTitle(error.title)
          }
          setApiError(error.message)
        }
      }
    } finally {
      setIsPending(false)
    }
  }

  return (
    <AuthFormShell
      onSubmit={handleSubmit}
      isPending={isPending}
      submitLabel={mode === "login" ? "Sign in" : "Create account"}
      apiError={apiError}
      apiErrorTitle={
        apiErrorTitle ??
        (mode === "login" ? "Sign in failed" : "Registration failed")
      }
    >
      <AuthFormField id="email" label="Email" error={fieldErrors.email}>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          aria-invalid={fieldErrors.email ? true : undefined}
          disabled={isPending}
          required
        />
      </AuthFormField>

      <AuthFormField
        id="password"
        label={
          <div className="flex items-center justify-between gap-3">
            <Label htmlFor="password">Password</Label>
            {mode === "login" ? (
              <Link
                to="/forgot-password"
                className="text-xs font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
              >
                Forgot password?
              </Link>
            ) : null}
          </div>
        }
        error={fieldErrors.password}
      >
        <Input
          id="password"
          type="password"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={fieldErrors.password ? true : undefined}
          disabled={isPending}
          required
        />
      </AuthFormField>
    </AuthFormShell>
  )
}

export { AuthForm }
