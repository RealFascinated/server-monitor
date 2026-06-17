import { useMutation } from "@tanstack/react-query"
import { useState } from "react"

import { rotateIngestToken } from "@/lib/api/user/servers"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type UseRotateIngestTokenOptions = {
  serverId: number
  errorTitle: string
  errorFallback: string
}

export function useRotateIngestToken({
  serverId,
  errorTitle,
  errorFallback,
}: UseRotateIngestTokenOptions) {
  const [ingestToken, setIngestToken] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => rotateIngestToken(serverId),
    onSuccess: (response) => {
      setIngestToken(response.ingestToken)
      toastSuccess("Token rotated")
    },
    onError: (error) => {
      toastMutationError(errorTitle, error, errorFallback)
    },
  })

  function resetToken() {
    setIngestToken(null)
    mutation.reset()
  }

  return { ingestToken, setIngestToken, mutation, resetToken }
}
