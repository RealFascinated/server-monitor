import type { Credentials } from "@/lib/auth/types"

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const MIN_PASSWORD_LENGTH = 8
export const MAX_PASSWORD_LENGTH = 128

export function validateEmail(email: string): string | null {
  if (!EMAIL_PATTERN.test(email.trim())) {
    return "Enter a valid email address"
  }

  return null
}

export function validateRequiredEmail(email: string): string | null {
  const trimmed = email.trim()

  if (!trimmed) {
    return "Email is required"
  }

  return validateEmail(trimmed)
}

export function validatePasswordConfirmation(
  password: string,
  confirmPassword: string
): string | null {
  if (password !== confirmPassword) {
    return "Passwords do not match"
  }

  return null
}

export function validateNewPassword(password: string): string | null {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return `Password must be at least ${MIN_PASSWORD_LENGTH} characters`
  }

  if (password.length > MAX_PASSWORD_LENGTH) {
    return `Password must be at most ${MAX_PASSWORD_LENGTH} characters`
  }

  return null
}

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
