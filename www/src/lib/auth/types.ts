export type UserRole = "ADMIN" | "USER"

export type User = {
  id: number
  email: string
  role: UserRole
  createdAt: string
}

export type AuthToken = {
  token: string
  expiresAt: string
}

export type ApiError = {
  status: string
  code: number
  message: string
  timestamp: string
}

export type Credentials = {
  email: string
  password: string
}
