import type { ReactNode } from "react"

import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { getApiErrorMessage, getApiErrorTitle } from "@/lib/api/error-message"

type QueryStatusState = {
  errorMessage: string | null
  errorTitle: string | null
  hasError: boolean
}

type UseQueryStatusOptions = {
  error: unknown
  fallbackMessage: string
  fallbackTitle: string
}

export function useQueryStatus({
  error,
  fallbackMessage,
  fallbackTitle,
}: UseQueryStatusOptions): QueryStatusState {
  const errorMessage = error
    ? getApiErrorMessage(error, fallbackMessage)
    : null
  const errorTitle = error ? getApiErrorTitle(error, fallbackTitle) : null

  return {
    errorMessage,
    errorTitle,
    hasError: errorMessage !== null,
  }
}

type QueryStatusShellProps = {
  error: unknown
  isPending: boolean
  loadingMessage: string
  fallbackMessage: string
  fallbackTitle: string
  children: ReactNode
  className?: string
}

function QueryStatusShell({
  error,
  isPending,
  loadingMessage,
  fallbackMessage,
  fallbackTitle,
  children,
  className,
}: QueryStatusShellProps) {
  const { errorMessage, errorTitle, hasError } = useQueryStatus({
    error,
    fallbackMessage,
    fallbackTitle,
  })

  return (
    <>
      {errorMessage ? (
        <Callout type="danger" title={errorTitle ?? fallbackTitle}>
          {errorMessage}
        </Callout>
      ) : null}

      {!hasError ? (
        <AsyncContent
          loading={isPending}
          loadingMessage={loadingMessage}
          className={className}
        >
          {children}
        </AsyncContent>
      ) : null}
    </>
  )
}

export { QueryStatusShell }
