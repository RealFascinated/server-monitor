import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { previewAlertRule } from "@/lib/api/user/alerts"
import type { AlertPreviewRequest } from "@/lib/api/user/alerts"
import { alertsQueryKey } from "@/lib/api/user/alerts.queries"

const DEBOUNCE_MS = 400

/** Runs the real alert state machine over recent history. Pass null to skip. */
export function useAlertPreview(request: AlertPreviewRequest | null) {
  const debounced = useDebouncedValue(request, DEBOUNCE_MS)

  return useQuery({
    queryKey: [...alertsQueryKey, "preview", debounced] as const,
    queryFn: () => previewAlertRule(debounced!),
    enabled: debounced != null,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: false,
  })
}
