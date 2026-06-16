import { useCallback, useState } from "react"

type FieldErrors<T extends string> = Partial<Record<T, string>>

export function useAuthForm<T extends string = string>() {
  const [fieldErrors, setFieldErrors] = useState<FieldErrors<T>>({})
  const [apiError, setApiError] = useState<string | null>(null)
  const [isPending, setIsPending] = useState(false)

  const clearErrors = useCallback(() => {
    setFieldErrors({})
    setApiError(null)
  }, [])

  const clearFieldErrors = useCallback(() => {
    setFieldErrors({})
  }, [])

  return {
    fieldErrors,
    setFieldErrors,
    apiError,
    setApiError,
    isPending,
    setIsPending,
    clearErrors,
    clearFieldErrors,
  }
}
