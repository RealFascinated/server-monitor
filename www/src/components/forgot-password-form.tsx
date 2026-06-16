import { useState } from "react"
import { useMutation } from "@tanstack/react-query"

import { Callout } from "@/components/callout"
import { AuthFormField, AuthFormShell } from "@/components/auth-form-shell"
import { Input } from "@/components/ui/input"
import { useAuthForm } from "@/hooks/use-auth-form"
import { forgotPassword } from "@/lib/api/auth/password"
import { validateEmail } from "@/lib/auth/validation"
import { toastMutationError } from "@/lib/toast"

function ForgotPasswordForm() {
  const [email, setEmail] = useState("")
  const [sent, setSent] = useState(false)
  const { fieldErrors, setFieldErrors, clearFieldErrors } =
    useAuthForm<"email">()

  const mutation = useMutation({
    mutationFn: forgotPassword,
    onSuccess: () => {
      setSent(true)
      clearFieldErrors()
    },
    onError: (error) => {
      toastMutationError("Could not send reset email", error, "Request failed")
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFieldErrors()

    const emailError = validateEmail(email)
    if (emailError) {
      setFieldErrors({ email: emailError })
      return
    }

    mutation.mutate({ email: email.trim() })
  }

  if (sent) {
    return (
      <Callout type="success" title="Check your email">
        If an account exists for that address, we sent a password reset link.
        The link expires in one hour.
      </Callout>
    )
  }

  return (
    <AuthFormShell
      onSubmit={handleSubmit}
      isPending={mutation.isPending}
      submitLabel="Send reset link"
    >
      <AuthFormField id="email" label="Email" error={fieldErrors.email}>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          aria-invalid={fieldErrors.email ? true : undefined}
          disabled={mutation.isPending}
          required
        />
      </AuthFormField>
    </AuthFormShell>
  )
}

export { ForgotPasswordForm }
