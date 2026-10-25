import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Check, Copy, Plus } from "lucide-react"
import { useState } from "react"

import { Callout } from "@/components/callout"
import { FormFieldError } from "@/components/form-field-error"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { env } from "@/env"
import { createApiToken } from "@/lib/api/user/api-tokens"
import type { CreatedApiToken } from "@/lib/api/user/api-tokens"
import {
  apiTokenScopesQueryOptions,
  apiTokensQueryKey,
} from "@/lib/api/user/api-tokens.queries"
import { copyWithToast, toastMutationError } from "@/lib/toast"

const MAX_NAME_LENGTH = 64
const NEVER_EXPIRES = "never"
const DEFAULT_EXPIRY = "90"
const DEFAULT_SCOPES = ["servers:read", "metrics:read", "alerts:read"]

const EXPIRY_OPTIONS = [
  { value: "30", label: "30 days" },
  { value: "90", label: "90 days" },
  { value: "365", label: "1 year" },
  { value: NEVER_EXPIRES, label: "Never" },
]

function CopyBlock({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    if (await copyWithToast(value)) {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="flex min-w-0 flex-col gap-2">
      <Label>{label}</Label>
      <div className="flex gap-2">
        <pre className="min-w-0 flex-1 rounded-md border border-border bg-muted px-3 py-2 font-mono text-xs break-all whitespace-pre-wrap">
          {value}
        </pre>
        <Button
          type="button"
          variant="outline"
          size="icon-sm"
          aria-label={`Copy ${label.toLowerCase()}`}
          onClick={handleCopy}
        >
          {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
        </Button>
      </div>
    </div>
  )
}

function CreateApiTokenDialog() {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState("")
  const [scopes, setScopes] = useState<string[]>(DEFAULT_SCOPES)
  const [expiry, setExpiry] = useState(DEFAULT_EXPIRY)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [created, setCreated] = useState<CreatedApiToken | null>(null)

  const queryClient = useQueryClient()
  const { data: availableScopes = [] } = useQuery({
    ...apiTokenScopesQueryOptions(),
    enabled: open,
  })

  const mutation = useMutation({
    mutationFn: createApiToken,
    onSuccess: async (token) => {
      await queryClient.invalidateQueries({ queryKey: apiTokensQueryKey })
      setCreated(token)
    },
    onError: (error) => {
      toastMutationError("Could not create token", error, "Request failed")
    },
  })

  function resetForm() {
    setName("")
    setScopes(DEFAULT_SCOPES)
    setExpiry(DEFAULT_EXPIRY)
    setFieldError(null)
    setCreated(null)
  }

  function handleOpenChange(nextOpen: boolean) {
    if (mutation.isPending) {
      return
    }
    setOpen(nextOpen)
    resetForm()
  }

  function toggleScope(id: string, checked: boolean) {
    setScopes((current) =>
      checked ? [...current, id] : current.filter((scope) => scope !== id)
    )
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed === "") {
      setFieldError("Enter a name")
      return
    }
    if (scopes.length === 0) {
      setFieldError("Select at least one scope")
      return
    }
    setFieldError(null)
    mutation.mutate({
      name: trimmed,
      scopes,
      expiresInDays: expiry === NEVER_EXPIRES ? null : Number(expiry),
    })
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm">
          <Plus className="size-4" />
          Create token
        </Button>
      </DialogTrigger>
      <DialogContent className="border border-border sm:max-w-xl">
        {created ? (
          <>
            <DialogHeader>
              <DialogTitle>Token created</DialogTitle>
              <DialogDescription>
                Copy the token now. It is only shown once.
              </DialogDescription>
            </DialogHeader>

            <div className="flex min-w-0 flex-col gap-4">
              <CopyBlock label="Token" value={created.token} />
              <CopyBlock
                label="Claude Code"
                value={`claude mcp add --transport http server-monitor ${env.VITE_API_URL}/mcp --header "Authorization: Bearer ${created.token}"`}
              />
              <Callout type="warning" title="Keep this token secret">
                Anyone with it can act as you within its scopes. Revoke it here
                if it leaks.
              </Callout>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="default"
                onClick={() => handleOpenChange(false)}
              >
                Done
              </Button>
            </DialogFooter>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle>Create API token</DialogTitle>
              <DialogDescription>
                Lets an MCP client such as Claude Code use your account. Grant
                only the scopes it needs.
              </DialogDescription>
            </DialogHeader>

            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="api-token-name">Name</Label>
                <Input
                  id="api-token-name"
                  value={name}
                  maxLength={MAX_NAME_LENGTH}
                  placeholder="Claude Code on my laptop"
                  onChange={(event) => setName(event.target.value)}
                  disabled={mutation.isPending}
                  autoFocus
                />
              </div>

              <fieldset className="flex flex-col gap-2">
                <Label asChild>
                  <legend>Scopes</legend>
                </Label>
                {availableScopes.map((scope) => (
                  <label
                    key={scope.id}
                    className="flex items-start gap-2.5 rounded-md border border-border px-3 py-2"
                  >
                    <input
                      type="checkbox"
                      className="mt-0.5 size-4 accent-primary"
                      checked={scopes.includes(scope.id)}
                      disabled={mutation.isPending}
                      onChange={(event) =>
                        toggleScope(scope.id, event.target.checked)
                      }
                    />
                    <span className="flex flex-col">
                      <span className="font-mono text-sm">{scope.id}</span>
                      <span className="text-xs text-muted-foreground">
                        {scope.description}
                      </span>
                    </span>
                  </label>
                ))}
              </fieldset>

              <div className="flex flex-col gap-2">
                <Label htmlFor="api-token-expiry">Expires</Label>
                <Select
                  value={expiry}
                  onValueChange={setExpiry}
                  disabled={mutation.isPending}
                >
                  <SelectTrigger id="api-token-expiry" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    className="w-(--radix-select-trigger-width)"
                  >
                    {EXPIRY_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <FormFieldError error={fieldError} />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="default"
                disabled={mutation.isPending}
                onClick={() => handleOpenChange(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="highlighted"
                disabled={mutation.isPending}
              >
                {mutation.isPending ? <Spinner /> : null}
                Create
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}

export { CreateApiTokenDialog }
