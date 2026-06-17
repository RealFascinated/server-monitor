import { useQuery } from "@tanstack/react-query"

import { Callout } from "@/components/callout"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { useAcceptServerInvite } from "@/hooks/use-accept-server-invite"
import { acceptServerInvite } from "@/lib/api/user/invites"
import { serverInvitePreviewQueryOptions } from "@/lib/api/user/invites.queries"
import { useAuth } from "@/lib/auth"
import { formatDate, formatServerRole } from "@/lib/formatter"
import { SERVER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"

type AcceptInviteViewProps = {
  token: string
}

function AcceptInviteView({ token }: AcceptInviteViewProps) {
  const { user } = useAuth()

  const {
    data: preview,
    isPending,
    error,
  } = useQuery(serverInvitePreviewQueryOptions(token))

  const acceptMutation = useAcceptServerInvite({
    mutationFn: () => acceptServerInvite({ token }),
    errorServerName: preview?.serverName,
  })

  const emailMismatch =
    preview && user
      ? preview.email.toLowerCase() !== user.email.toLowerCase()
      : false

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6">
      <QueryStatusShell
        error={error}
        isPending={isPending}
        loadingMessage="Loading invite…"
        fallbackMessage="Could not load invite"
        fallbackTitle="Invalid invite"
        className="flex flex-col gap-6"
      >
        {preview ? (
          <>
            <div className="flex flex-col gap-2 text-center">
              <h1>Join {preview.serverName}</h1>
              <p className="text-sm text-muted-foreground">
                <span className="text-foreground">
                  {preview.invitedByEmail}
                </span>{" "}
                invited you to join as a{" "}
                <span title={SERVER_ROLE_TOOLTIPS[preview.role]}>
                  {formatServerRole(preview.role)}
                </span>
                .
              </p>
              <p className="text-sm text-muted-foreground">
                Sent to {preview.email}. Expires{" "}
                {formatDate(preview.expiresAt)}.
              </p>
            </div>

            {emailMismatch ? (
              <Callout type="warning" title="Wrong account">
                This invite was sent to {preview.email}, but you are signed in
                as {user?.email}. Sign in with the invited account to accept.
              </Callout>
            ) : null}

            <Button
              type="button"
              variant="highlighted"
              className="w-full"
              disabled={acceptMutation.isPending || emailMismatch}
              onClick={() => {
                acceptMutation.mutate()
              }}
            >
              {acceptMutation.isPending ? <Spinner /> : null}
              Accept invite
            </Button>
          </>
        ) : null}
      </QueryStatusShell>
    </div>
  )
}

export { AcceptInviteView }
