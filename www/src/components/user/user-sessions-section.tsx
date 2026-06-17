import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { LogOut, Trash2 } from "lucide-react"

import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
import {
  revokeOtherUserSessions,
  revokeUserSession,
} from "@/lib/api/user/sessions"
import type { UserSession } from "@/lib/api/user/sessions"
import { userSessionsQueryKey, userSessionsQueryOptions } from "@/lib/api/user/sessions.queries"
import { getApiErrorMessage, getApiErrorTitle } from "@/lib/api/error-message"
import { formatDate, formatDateWithRelative } from "@/lib/formatter"
import { toastMutationError, toastSuccess } from "@/lib/toast"
import { cn } from "@/lib/utils"

function RevokeSessionButton({ session }: { session: UserSession }) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => revokeUserSession(session.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userSessionsQueryKey })
      toastSuccess("Session revoked")
    },
    onError: (error) => {
      toastMutationError("Could not revoke session", error, "Request failed")
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="text-muted-foreground hover:bg-transparent hover:text-red-600 dark:hover:text-red-400"
          aria-label="Revoke session"
        >
          <Trash2 className="size-4" />
        </Button>
      }
      title="Revoke session"
      triggerTooltip="Revoke session"
      description="Sign out this device? It will need to sign in again."
      confirmLabel="Revoke"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

function RevokeOtherSessionsButton({ disabled }: { disabled: boolean }) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: revokeOtherUserSessions,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userSessionsQueryKey })
      toastSuccess("Other sessions revoked")
    },
    onError: (error) => {
      toastMutationError(
        "Could not revoke other sessions",
        error,
        "Request failed"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={disabled || mutation.isPending}
        >
          <LogOut className="size-4" />
          Sign out other devices
        </Button>
      }
      title="Sign out other devices"
      description="End every other active session. This device stays signed in."
      confirmLabel="Sign out others"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

function UserSessionsSection() {
  const sessionsQuery = useQuery(userSessionsQueryOptions())
  const sessions = sessionsQuery.data ?? []
  const otherSessions = sessions.filter((session) => !session.current)
  const errorMessage = sessionsQuery.error
    ? getApiErrorMessage(sessionsQuery.error, "Failed to load sessions")
    : null
  const errorTitle = sessionsQuery.error
    ? getApiErrorTitle(sessionsQuery.error, "Could not load sessions")
    : null

  return (
    <div className="flex max-w-xl flex-col gap-4">
      {errorMessage ? (
        <Callout type="danger" title={errorTitle ?? "Could not load sessions"}>
          {errorMessage}
        </Callout>
      ) : null}

      {!errorMessage ? (
        <AsyncContent
          loading={sessionsQuery.isPending}
          loadingMessage="Loading sessions…"
        >
          {sessions.length === 0 ? (
            <p className="text-sm text-muted-foreground">No active sessions.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {sessions.map((session) => (
                <li
                  key={session.id}
                  className={cn(
                    "flex items-center justify-between gap-3 rounded-sm border border-border bg-background px-3 py-2",
                    session.current && "border-primary/30"
                  )}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">
                        {session.current ? "This device" : "Signed-in device"}
                      </p>
                      {session.current ? (
                        <span className="bg-primary/10 px-2 py-0.5 text-xs font-bold text-primary">
                          Current
                        </span>
                      ) : null}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      <SimpleTooltip
                        content={formatDateWithRelative(session.createdAt)}
                      >
                        <span className="cursor-help">
                          Signed in {formatDate(session.createdAt)}
                        </span>
                      </SimpleTooltip>
                      {" · "}
                      <SimpleTooltip
                        content={formatDateWithRelative(session.expiresAt)}
                      >
                        <span className="cursor-help">
                          Expires {formatDate(session.expiresAt)}
                        </span>
                      </SimpleTooltip>
                    </p>
                  </div>

                  {session.current ? null : (
                    <RevokeSessionButton session={session} />
                  )}
                </li>
              ))}
            </ul>
          )}
        </AsyncContent>
      ) : null}

      <RevokeOtherSessionsButton disabled={otherSessions.length === 0} />
    </div>
  )
}

export { UserSessionsSection }
