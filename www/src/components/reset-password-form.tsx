import { useState } from "react"
import { useMutation } from "@tanstack/react-query"
import { useNavigate } from "@tanstack/react-router"

import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { resetPassword } from "@/lib/api/auth/password"
import { validateNewPassword } from "@/lib/auth/validation"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type ResetPasswordFormProps = {
  token: string
}

function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const navigate = useNavigate()
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [fieldErrors, setFieldErrors] = useState<{
    password?: string
    confirmPassword?: string
  }>({})

  const mutation = useMutation({
    mutationFn: resetPassword,
    onSuccess: () => {
      toastSuccess("Password updated. Sign in with your new password.")
      void navigate({ to: "/login" })
    },
    onError: (error) => {
      toastMutationError("Could not reset password", error, "Request failed")
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFieldErrors({})

    if (password !== confirmPassword) {
      setFieldErrors({ confirmPassword: "Passwords do not match" })
      return
    }

    const passwordError = validateNewPassword(password)
    if (passwordError) {
      setFieldErrors({ password: passwordError })
      return
    }

    mutation.mutate({ token, password })
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="new-password">New password</Label>
        <Input
          id="new-password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={fieldErrors.password ? true : undefined}
          disabled={mutation.isPending}
          required
        />
        {fieldErrors.password ? (
          <p className="text-xs font-bold text-error">{fieldErrors.password}</p>
        ) : null}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="confirm-password">Confirm new password</Label>
        <Input
          id="confirm-password"
          type="password"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          aria-invalid={fieldErrors.confirmPassword ? true : undefined}
          disabled={mutation.isPending}
          required
        />
        {fieldErrors.confirmPassword ? (
          <p className="text-xs font-bold text-error">
            {fieldErrors.confirmPassword}
          </p>
        ) : null}
      </div>

      <Button type="submit" variant="highlighted" disabled={mutation.isPending}>
        {mutation.isPending ? <Spinner /> : null}
        Reset password
      </Button>
    </form>
  )
}

export { ResetPasswordForm }
