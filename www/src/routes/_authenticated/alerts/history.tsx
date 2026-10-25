import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import { AlertHistoryView } from "@/components/alerts/alert-history-view"
import { pageTitle } from "@/lib/page-title"
import { DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE } from "@/lib/schemas/pagination"

const historySearchSchema = z.object({
  page: z.coerce.number().int().min(1).optional(),
  count: z.coerce.number().int().min(1).max(MAX_PAGE_SIZE).optional(),
  state: z.enum(["firing", "resolved"]).optional(),
  groupId: z.coerce.number().int().optional(),
  serverId: z.coerce.number().int().optional(),
})

export const Route = createFileRoute("/_authenticated/alerts/history")({
  validateSearch: historySearchSchema,
  head: () => ({
    meta: [{ title: pageTitle("Alert history") }],
  }),
  component: AlertHistoryRoute,
})

function AlertHistoryRoute() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const pagination = {
    page: search.page ?? 1,
    count: search.count ?? DEFAULT_PAGE_SIZE,
  }

  function update(next: Partial<typeof search>) {
    void navigate({
      search: (prev) => ({ ...prev, page: 1, ...next }),
      resetScroll: false,
    })
  }

  return (
    <AlertHistoryView
      pagination={pagination}
      filters={{
        state: search.state,
        groupId: search.groupId,
        serverId: search.serverId,
      }}
      onFiltersChange={update}
      onPageChange={(page) =>
        navigate({ search: (prev) => ({ ...prev, page }), resetScroll: false })
      }
      onPageSizeChange={(count) =>
        navigate({
          search: (prev) => ({ ...prev, page: 1, count }),
          resetScroll: false,
        })
      }
    />
  )
}
