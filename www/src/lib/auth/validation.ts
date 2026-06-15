import type { Credentials } from "@/lib/auth/types"

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateCredentials(
  data: unknown,
  options: { minPasswordLength?: number } = {}
): Credentials {
  if (typeof data !== "object" || data === null) {
    throw new Error("Invalid credentials")
  }

  const { email, password } = data as Record<string, unknown>

  if (typeof email !== "string" || !EMAIL_PATTERN.test(email.trim())) {
    throw new Error("Enter a valid email address")
  }

  if (typeof password !== "string" || password.length === 0) {
    throw new Error("Password is required")
  }

  const minPasswordLength = options.minPasswordLength ?? 1
  if (password.length < minPasswordLength) {
    throw new Error(`Password must be at least ${minPasswordLength} characters`)
  }

  if (password.length > 128) {
    throw new Error("Password must be at most 128 characters")
  }

  return {
    email: email.trim(),
    password,
  }
}
