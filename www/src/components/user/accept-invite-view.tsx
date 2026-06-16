import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "@tanstack/react-router"

import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import { acceptServerInvite } from "@/lib/api/user/invites"
import {
  serverInvitePreviewQueryOptions,
  userInvitesQueryKey,
} from "@/lib/api/user/invites.queries"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { useAuth } from "@/lib/auth"
import { formatDate } from "@/lib/formatter"
import { toastMutationError } from "@/lib/toast"
import { SERVER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"

type AcceptInviteViewProps = {
  token: string
}

function formatRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase()
}

function AcceptInviteView({ token }: AcceptInviteViewProps) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { defaultRange } = useMetricDefaultRange()

  const {
    data: preview,
    isPending,
    error,
  } = useQuery(serverInvitePreviewQueryOptions(token))

  const acceptMutation = useMutation({
    mutationFn: () => acceptServerInvite({ token }),
    onSuccess: async (member) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userServersQueryKey }),
        queryClient.invalidateQueries({ queryKey: userInvitesQueryKey }),
      ])
      await navigate({
        to: "/servers/$serverId",
        params: { serverId: String(member.serverId) },
        search: { range: defaultRange },
      })
    },
    onError: (mutationError) => {
      toastMutationError(
        preview
          ? `Could not accept invite to ${preview.serverName}`
          : "Could not accept invite",
        mutationError,
        "Failed to accept invite"
      )
    },
  })

  const previewError =
    error instanceof Error ? error.message : error ? "Could not load invite" : null
  const emailMismatch =
    preview && user
      ? preview.email.toLowerCase() !== user.email.toLowerCase()
      : false

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6">
      {previewError ? (
        <Callout type="danger" title="Invalid invite">
          {previewError}
        </Callout>
      ) : null}

      {!previewError ? (
        <AsyncContent
          loading={isPending}
          loadingMessage="Loading invite…"
          className="flex flex-col gap-6"
        >
          {preview ? (
            <>
              <div className="flex flex-col gap-2 text-center">
                <h1>Join {preview.serverName}</h1>
                <p className="text-sm text-muted-foreground">
                  <span className="text-foreground">{preview.invitedByEmail}</span>{" "}
                  invited you to join as a{" "}
                  <span title={SERVER_ROLE_TOOLTIPS[preview.role]}>
                    {formatRole(preview.role)}
                  </span>
                  .
                </p>
                <p className="text-sm text-muted-foreground">
                  Sent to {preview.email}. Expires {formatDate(preview.expiresAt)}.
                </p>
              </div>

              {emailMismatch ? (
                <Callout type="warning" title="Wrong account">
                  This invite was sent to {preview.email}, but you are signed in as{" "}
                  {user?.email}. Sign in with the invited account to accept.
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
        </AsyncContent>
      ) : null}
    </div>
  )
}

export { AcceptInviteView }
