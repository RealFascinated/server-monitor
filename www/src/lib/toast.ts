import { toast } from "sonner"

import { getApiErrorMessage } from "@/lib/api/error-message"
import { copyToClipboard } from "@/lib/clipboard"

const COPY_FAILED_MESSAGE =
  "Could not copy to clipboard. Check browser permissions or use HTTPS."

function getMutationErrorMessage(error: unknown, fallback: string): string {
  return getApiErrorMessage(error, fallback)
}

function toastMutationError(
  title: string,
  error: unknown,
  fallback: string
): void {
  toast.error(title, {
    description: getMutationErrorMessage(error, fallback),
  })
}

function toastSuccess(message: string): void {
  toast.success(message)
}

async function copyWithToast(text: string): Promise<boolean> {
  const didCopy = await copyToClipboard(text)

  if (didCopy) {
    toastSuccess("Copied to clipboard")
  } else {
    toast.error("Copy failed", { description: COPY_FAILED_MESSAGE })
  }

  return didCopy
}

export {
  copyWithToast,
  getMutationErrorMessage,
  toastMutationError,
  toastSuccess,
}
