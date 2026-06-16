import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useCallback, useState } from "react"

import { renameServer } from "@/lib/api/user/servers"
import { updateServerInCaches } from "@/lib/api/user/servers.queries"
import { validateServerName } from "@/lib/server-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type UseRenameServerOptions = {
  serverId: number
  currentName: string
  onSuccess?: () => void
}

type SubmitOptions = {
  onUnchanged?: () => void
}

export function useRenameServer({
  serverId,
  currentName,
  onSuccess,
}: UseRenameServerOptions) {
  const [name, setName] = useState(currentName)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const inputId = `rename-server-name-${serverId}`

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (nextName: string) =>
      renameServer(serverId, { name: nextName }),
    onSuccess: (server) => {
      updateServerInCaches(queryClient, server)
      setFieldError(null)
      toastSuccess("Server renamed")
      onSuccess?.()
    },
    onError: (error) => {
      toastMutationError(
        "Could not rename server",
        error,
        "Failed to rename server"
      )
    },
  })

  const trimmedName = name.trim()
  const isUnchanged = trimmedName === currentName
  const canSave =
    !mutation.isPending && !isUnchanged && validateServerName(name) === null

  const resetForm = useCallback(() => {
    setName(currentName)
    setFieldError(null)
  }, [currentName])

  function submit(
    event: React.FormEvent<HTMLFormElement>,
    options?: SubmitOptions
  ) {
    event.preventDefault()

    const error = validateServerName(name)
    if (error) {
      setFieldError(error)
      return
    }

    if (isUnchanged) {
      options?.onUnchanged?.()
      return
    }

    setFieldError(null)
    mutation.mutate(trimmedName)
  }

  return {
    name,
    setName,
    fieldError,
    setFieldError,
    inputId,
    mutation,
    resetForm,
    submit,
    canSave,
  }
}
