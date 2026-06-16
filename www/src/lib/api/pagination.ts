export type Page<T> = {
  items: T[]
  totalItems: number
  itemsPerPage: number
  totalPages: number
}

export type PageParams = {
  page: number
  count: number
}

export function emptyPage<T>(count: number): Page<T> {
  return {
    items: [],
    totalItems: 0,
    itemsPerPage: count,
    totalPages: 0,
  }
}

export function pageItemRange(
  page: Page<unknown>,
  currentPage: number
): { start: number; end: number } | null {
  if (page.totalItems === 0) {
    return null
  }

  const start = (currentPage - 1) * page.itemsPerPage + 1
  const end = Math.min(currentPage * page.itemsPerPage, page.totalItems)

  return { start, end }
}

export function getVisiblePageNumbers(
  currentPage: number,
  totalPages: number
): (number | "ellipsis")[] {
  if (totalPages <= 0) {
    return []
  }

  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }

  const pages = new Set<number>([1, totalPages, currentPage])

  if (currentPage > 1) {
    pages.add(currentPage - 1)
  }

  if (currentPage < totalPages) {
    pages.add(currentPage + 1)
  }

  const sorted = [...pages].sort((a, b) => a - b)
  const result: (number | "ellipsis")[] = []

  for (let index = 0; index < sorted.length; index++) {
    const page = sorted[index]

    if (index > 0 && page - sorted[index - 1] > 1) {
      result.push("ellipsis")
    }

    result.push(page)
  }

  return result
}
