import { useState } from "react"
import { useMutation } from "@tanstack/react-query"

import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { changePassword } from "@/lib/api/user/account"
import { toastMutationError, toastSuccess } from "@/lib/toast"

const MIN_PASSWORD_LENGTH = 8
const MAX_PASSWORD_LENGTH = 128

function validateNewPassword(password: string): string | null {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return `Password must be at least ${MIN_PASSWORD_LENGTH} characters`
  }

  if (password.length > MAX_PASSWORD_LENGTH) {
    return `Password must be at most ${MAX_PASSWORD_LENGTH} characters`
  }

  return null
}

function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [fieldErrors, setFieldErrors] = useState<{
    currentPassword?: string
    newPassword?: string
    confirmPassword?: string
  }>({})

  const mutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      toastSuccess("Password updated")
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
      setFieldErrors({})
    },
    onError: (error) => {
      toastMutationError("Could not change password", error, "Request failed")
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFieldErrors({})

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
    <form className="flex max-w-md flex-col gap-4" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="current-password">Current password</Label>
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
        {fieldErrors.currentPassword ? (
          <p className="text-xs font-bold text-error">
            {fieldErrors.currentPassword}
          </p>
        ) : null}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="new-password">New password</Label>
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
        {fieldErrors.newPassword ? (
          <p className="text-xs font-bold text-error">
            {fieldErrors.newPassword}
          </p>
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

      <Button
        type="submit"
        variant="highlighted"
        disabled={mutation.isPending}
        className="self-start"
      >
        {mutation.isPending ? <Spinner /> : null}
        Update password
      </Button>
    </form>
  )
}

export { ChangePasswordForm }
