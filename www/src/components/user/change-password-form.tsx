import { useState } from "react"
import { useMutation } from "@tanstack/react-query"

import { AuthFormField, AuthFormShell } from "@/components/auth-form-shell"
import { Input } from "@/components/ui/input"
import { useAuthForm } from "@/hooks/use-auth-form"
import { changePassword } from "@/lib/api/user/account"
import { validateNewPassword } from "@/lib/auth/validation"
import { toastMutationError, toastSuccess } from "@/lib/toast"

function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const { fieldErrors, setFieldErrors, clearFieldErrors } = useAuthForm<
    "currentPassword" | "newPassword" | "confirmPassword"
  >()

  const mutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      toastSuccess("Password updated")
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
      clearFieldErrors()
    },
    onError: (error) => {
      toastMutationError("Could not change password", error, "Request failed")
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFieldErrors()

    if (newPassword !== confirmPassword) {
      setFieldErrors({ confirmPassword: "Passwords do not match" })
      return
    }

    if (!currentPassword) {
      setFieldErrors({ currentPassword: "Current password is required" })
      return
    }

    const passwordError = validateNewPassword(newPassword)
    if (passwordError) {
      setFieldErrors({ newPassword: passwordError })
      return
    }

    mutation.mutate({
      currentPassword,
      newPassword,
    })
  }

  return (
    <AuthFormShell
      onSubmit={handleSubmit}
      isPending={mutation.isPending}
      submitLabel="Update password"
      className="max-w-md"
      submitClassName="self-start"
    >
      <AuthFormField
        id="current-password"
        label="Current password"
        error={fieldErrors.currentPassword}
      >
        <Input
          id="current-password"
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          aria-invalid={fieldErrors.currentPassword ? true : undefined}
          disabled={mutation.isPending}
          required
        />
      </AuthFormField>

      <AuthFormField
        id="new-password"
        label="New password"
        error={fieldErrors.newPassword}
      >
        <Input
          id="new-password"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          aria-invalid={fieldErrors.newPassword ? true : undefined}
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

export { ChangePasswordForm }
