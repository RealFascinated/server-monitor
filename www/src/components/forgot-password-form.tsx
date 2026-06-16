import { useState } from "react"
import { useMutation } from "@tanstack/react-query"

import { Callout } from "@/components/callout"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { forgotPassword } from "@/lib/api/auth/password"
import { validateEmail } from "@/lib/auth/validation"
import { toastMutationError } from "@/lib/toast"

function ForgotPasswordForm() {
  const [email, setEmail] = useState("")
  const [fieldErrors, setFieldErrors] = useState<{ email?: string }>({})
  const [sent, setSent] = useState(false)

  const mutation = useMutation({
    mutationFn: forgotPassword,
    onSuccess: () => {
      setSent(true)
      setFieldErrors({})
    },
    onError: (error) => {
      toastMutationError("Could not send reset email", error, "Request failed")
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFieldErrors({})

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
    <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
      <div className="flex flex-col gap-2">
        <Label htmlFor="email">Email</Label>
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
        {fieldErrors.email ? (
          <p className="text-xs font-bold text-error">{fieldErrors.email}</p>
        ) : null}
      </div>

      <Button type="submit" variant="highlighted" disabled={mutation.isPending}>
        {mutation.isPending ? <Spinner /> : null}
        Send reset link
      </Button>
    </form>
  )
}

export { ForgotPasswordForm }
