export const MAX_SERVER_NAME_LENGTH = 64

export function validateServerName(name: string): string | null {
  const trimmed = name.trim()

  if (!trimmed) {
    return "Name must not be empty"
  }

  if (trimmed.length > MAX_SERVER_NAME_LENGTH) {
    return `Name must be at most ${MAX_SERVER_NAME_LENGTH} characters`
  }

  return null
}
