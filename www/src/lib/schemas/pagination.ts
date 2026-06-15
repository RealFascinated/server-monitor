import { z } from "zod"

export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 50
export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

export type PageSearchParams = {
  page: number
  count: number
}

type PageSearchSchemaOptions = {
  defaultCount?: number
  maxCount?: number
}

export function pageSearchSchema(options: PageSearchSchemaOptions = {}) {
  const defaultCount = options.defaultCount ?? DEFAULT_PAGE_SIZE
  const maxCount = options.maxCount ?? MAX_PAGE_SIZE

  return z
    .object({
      page: z.coerce.number().int().min(1).optional(),
      count: z.coerce.number().int().min(1).max(maxCount).optional(),
    })
    .transform(
      (search): PageSearchParams => ({
        page: search.page ?? 1,
        count: search.count ?? defaultCount,
      })
    )
}
