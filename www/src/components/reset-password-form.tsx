import { useState } from "react"
import { useMutation } from "@tanstack/react-query"
import { useNavigate } from "@tanstack/react-router"

import { AuthFormField, AuthFormShell } from "@/components/auth-form-shell"
import { Input } from "@/components/ui/input"
import { useAuthForm } from "@/hooks/use-auth-form"
import { resetPassword } from "@/lib/api/auth/password"
import { validateNewPassword, validatePasswordConfirmation } from "@/lib/auth/validation"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type ResetPasswordFormProps = {
  token: string
}

function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const navigate = useNavigate()
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const { fieldErrors, setFieldErrors, clearFieldErrors } = useAuthForm<
    "password" | "confirmPassword"
  >()

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
    clearFieldErrors()

    const confirmError = validatePasswordConfirmation(password, confirmPassword)
    if (confirmError) {
      setFieldErrors({ confirmPassword: confirmError })
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
    <AuthFormShell
      onSubmit={handleSubmit}
      isPending={mutation.isPending}
      submitLabel="Reset password"
    >
      <AuthFormField
        id="new-password"
        label="New password"
        error={fieldErrors.password}
      >
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
      </AuthFormField>

      <AuthFormField
        id="confirm-password"
        label="Confirm new password"
        error={fieldErrors.confirmPassword}
      >
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
      </AuthFormField>
    </AuthFormShell>
  )
}

export { ResetPasswordForm }
