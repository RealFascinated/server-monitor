import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { KeyRound, Trash2 } from "lucide-react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { QueryStatusShell } from "@/components/query-status-shell"
import { SettingsSubsectionHeader } from "@/components/settings/settings-subsection-header"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
import { CreateApiTokenDialog } from "@/components/user/create-api-token-dialog"
import { revokeApiToken } from "@/lib/api/user/api-tokens"
import type { ApiToken } from "@/lib/api/user/api-tokens"
import {
  apiTokensQueryKey,
  apiTokensQueryOptions,
} from "@/lib/api/user/api-tokens.queries"
import { formatRelativeTime } from "@/lib/formatter"
import { toastMutationError, toastSuccess } from "@/lib/toast"

function tokenSubtitle(token: ApiToken) {
  const used = token.lastUsedAt
    ? `Used ${formatRelativeTime(token.lastUsedAt)}`
    : "Never used"
  const expires = token.expiresAt
    ? `Expires ${formatRelativeTime(token.expiresAt)}`
    : "Never expires"
  return `${used} · ${expires}`
}

function RevokeApiTokenButton({ token }: { token: ApiToken }) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => revokeApiToken(token.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: apiTokensQueryKey })
      toastSuccess("Token revoked")
    },
    onError: (error) => {
      toastMutationError("Could not revoke token", error, "Request failed")
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
          aria-label="Revoke token"
        >
          <Trash2 className="size-4" />
        </Button>
      }
      title="Revoke token"
      triggerTooltip="Revoke token"
      description={`Clients using "${token.name}" will stop working immediately.`}
      confirmLabel="Revoke"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

function ApiTokensSection() {
  const tokensQuery = useQuery(apiTokensQueryOptions())
  const tokens = tokensQuery.data ?? []

  return (
    <div className="flex flex-col gap-3">
      <SettingsSubsectionHeader
        title="API tokens"
        description="Let MCP clients such as Claude Code read your servers and manage alerts."
        action={
          !tokensQuery.error && !tokensQuery.isPending ? (
            <CreateApiTokenDialog />
          ) : null
        }
      />

      <QueryStatusShell
        error={tokensQuery.error}
        isPending={tokensQuery.isPending}
        loadingMessage="Loading tokens…"
        fallbackMessage="Failed to load tokens"
        fallbackTitle="Could not load tokens"
      >
        {tokens.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border bg-muted/30 px-4 py-6 text-center">
            <p className="text-sm text-muted-foreground">No API tokens.</p>
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {tokens.map((token) => (
              <li
                key={token.id}
                className="flex items-center gap-3 rounded-lg border border-border bg-card px-3 py-2.5"
              >
                <div className="flex size-8 shrink-0 items-center justify-center rounded-sm bg-muted text-muted-foreground">
                  <KeyRound className="size-4" />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-medium">{token.name}</p>
                    <span className="font-mono text-xs text-muted-foreground">
                      {token.prefix}…
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {token.scopes.join(", ")}
                  </p>
                  <SimpleTooltip content={tokenSubtitle(token)}>
                    <p className="text-xs text-muted-foreground">
                      {tokenSubtitle(token)}
                    </p>
                  </SimpleTooltip>
                </div>

                <RevokeApiTokenButton token={token} />
              </li>
            ))}
          </ul>
        )}
      </QueryStatusShell>
    </div>
  )
}

export { ApiTokensSection }
