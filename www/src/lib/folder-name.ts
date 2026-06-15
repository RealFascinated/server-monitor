export const MAX_FOLDER_NAME_LENGTH = 20

export function validateFolderName(name: string): string | null {
  const trimmed = name.trim()

  if (!trimmed) {
    return "Folder name must not be empty"
  }

  if (trimmed.length > MAX_FOLDER_NAME_LENGTH) {
    return `Folder name must be at most ${MAX_FOLDER_NAME_LENGTH} characters`
  }

  return null
}
