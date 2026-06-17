import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { LogOut, Monitor, Trash2 } from "lucide-react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { QueryStatusShell } from "@/components/query-status-shell"
import { SettingsSubsectionHeader } from "@/components/settings/settings-subsection-header"
import { TimestampCell } from "@/components/timestamp-cell"
import { Button } from "@/components/ui/button"
import {
  revokeOtherUserSessions,
  revokeUserSession,
} from "@/lib/api/user/sessions"
import type { UserSession } from "@/lib/api/user/sessions"
import { userSessionsQueryKey, userSessionsQueryOptions } from "@/lib/api/user/sessions.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"
import { cn } from "@/lib/utils"

function sessionCountLabel(count: number) {
  if (count === 1) return "1 active session"
  return `${count} active sessions`
}

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
          className="text-muted-foreground hover:bg-transparent hover:text-error"
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
          Sign out others
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
  const sessionDescription =
    sessions.length > 0
      ? sessionCountLabel(sessions.length)
      : "Devices currently signed in to your account."

  return (
    <div className="flex flex-col gap-3">
      <SettingsSubsectionHeader
        title="Active sessions"
        description={sessionDescription}
        action={
          !sessionsQuery.error && !sessionsQuery.isPending ? (
            <RevokeOtherSessionsButton disabled={otherSessions.length === 0} />
          ) : null
        }
      />

      <QueryStatusShell
        error={sessionsQuery.error}
        isPending={sessionsQuery.isPending}
        loadingMessage="Loading sessions…"
        fallbackMessage="Failed to load sessions"
        fallbackTitle="Could not load sessions"
      >
        {sessions.length === 0 ? (
          <div className="rounded-sm border border-dashed border-border bg-muted/30 px-4 py-6 text-center">
            <p className="text-sm text-muted-foreground">No active sessions.</p>
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {sessions.map((session) => (
              <li
                key={session.id}
                className={cn(
                  "flex items-center gap-3 rounded-sm border border-border bg-card px-3 py-2.5",
                  session.current && "border-primary/30 bg-primary/5"
                )}
              >
                <div
                  className={cn(
                    "flex size-8 shrink-0 items-center justify-center rounded-sm",
                    session.current
                      ? "bg-primary/10 text-primary"
                      : "bg-muted text-muted-foreground"
                  )}
                >
                  <Monitor className="size-4" />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-medium">
                      {session.current ? "This device" : "Other device"}
                    </p>
                    {session.current ? (
                      <span className="rounded-sm bg-primary/10 px-1.5 py-0.5 text-[10px] font-semibold tracking-wide text-primary uppercase">
                        Current
                      </span>
                    ) : null}
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Signed in <TimestampCell iso={session.createdAt} />
                    {" · "}
                    Expires <TimestampCell iso={session.expiresAt} />
                  </p>
                </div>

                {session.current ? null : (
                  <RevokeSessionButton session={session} />
                )}
              </li>
            ))}
          </ul>
        )}
      </QueryStatusShell>
    </div>
  )
}

export { UserSessionsSection }
