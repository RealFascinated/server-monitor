import { useState } from "react"
import { Link, useNavigate } from "@tanstack/react-router"

import { Callout } from "@/components/callout"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { login, register, useAuth, validateCredentials } from "@/lib/auth"

type AuthFormProps = {
  mode: "login" | "register"
}

function AuthForm({ mode }: AuthFormProps) {
  const navigate = useNavigate()
  const { setUser } = useAuth()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [fieldErrors, setFieldErrors] = useState<{
    email?: string
    password?: string
  }>({})
  const [apiError, setApiError] = useState<string | null>(null)
  const [isPending, setIsPending] = useState(false)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setApiError(null)
    setFieldErrors({})

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
          setApiError(error.message)
        }
      }
    } finally {
      setIsPending(false)
    }
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
      {apiError ? (
        <Callout
          type="danger"
          title={mode === "login" ? "Sign in failed" : "Registration failed"}
        >
          {apiError}
        </Callout>
      ) : null}

      <div className="flex flex-col gap-2">
        <Label htmlFor="email">Email</Label>
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
        {fieldErrors.email ? (
          <p className="text-xs font-bold text-error">{fieldErrors.email}</p>
        ) : null}
      </div>

      <div className="flex flex-col gap-2">
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
        {fieldErrors.password ? (
          <p className="text-xs font-bold text-error">{fieldErrors.password}</p>
        ) : null}
      </div>

      <Button type="submit" variant="highlighted" disabled={isPending}>
        {isPending ? <Spinner /> : null}
        {mode === "login" ? "Sign in" : "Create account"}
      </Button>
    </form>
  )
}

export { AuthForm }
