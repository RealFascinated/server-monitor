const STORAGE_KEY = "monitor_session"

type StoredSession = {
  token: string
  expiresAt: string
}

export function getToken(): string | null {
  if (typeof window === "undefined") {
    return null
  }

  const raw = getStorage()?.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const session = JSON.parse(raw) as StoredSession
    if (new Date(session.expiresAt) <= new Date()) {
      clearToken()
      return null
    }

    return session.token
  } catch {
    clearToken()
    return null
  }
}

function getStorage(): Storage | null {
  if (typeof window === "undefined") {
    return null
  }

  return window.localStorage
}

export function setSession(token: string, expiresAt: string): void {
  const storage = getStorage()
  if (!storage) {
    return
  }

  const payload: StoredSession = { token, expiresAt }
  storage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

export function clearToken(): void {
  getStorage()?.removeItem(STORAGE_KEY)
}

export function hasToken(): boolean {
  return getToken() !== null
}
