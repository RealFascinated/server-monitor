export type NetworkErrorKind = "offline" | "unreachable"

export class NetworkError extends Error {
  readonly kind: NetworkErrorKind
  readonly title: string

  constructor(kind: NetworkErrorKind) {
    const title = kind === "offline" ? "You're offline" : "Connection failed"
    const message =
      kind === "offline"
        ? "Check your connection and try again."
        : "Couldn't reach the server. Check your connection and try again."

    super(message)
    this.name = "NetworkError"
    this.kind = kind
    this.title = title
  }
}

export function isNetworkError(error: unknown): error is NetworkError {
  return error instanceof NetworkError
}

export function createNetworkError(): NetworkError {
  if (typeof navigator !== "undefined" && !navigator.onLine) {
    return new NetworkError("offline")
  }

  return new NetworkError("unreachable")
}
