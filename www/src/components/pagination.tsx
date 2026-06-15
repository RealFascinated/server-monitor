import { ChevronLeft, ChevronRight } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { Page } from "@/lib/api/pagination"
import {
  getVisiblePageNumbers,
  pageItemRange,
} from "@/lib/api/pagination"
import { formatCount } from "@/lib/formatter"
import { PAGE_SIZE_OPTIONS } from "@/lib/schemas/pagination"
import { cn } from "@/lib/utils"

type PaginationProps = {
  page: Page<unknown>
  currentPage: number
  pageSize: number
  pageSizeOptions?: readonly number[]
  onPageChange: (page: number) => void
  onPageSizeChange: (count: number) => void
  itemLabel: string
  className?: string
}

function pluralize(count: number, label: string): string {
  return count === 1 ? label : `${label}s`
}

function Pagination({
  page,
  currentPage,
  pageSize,
  pageSizeOptions = PAGE_SIZE_OPTIONS,
  onPageChange,
  onPageSizeChange,
  itemLabel,
  className,
}: PaginationProps) {
  const range = pageItemRange(page, currentPage)
  const canGoBack = currentPage > 1
  const canGoForward = currentPage < page.totalPages
  const pageNumbers = getVisiblePageNumbers(currentPage, page.totalPages)

  if (!range) {
    return null
  }

  return (
    <nav
      aria-label="Pagination"
      className={cn(
        "flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between",
        className
      )}
    >
      <div className="flex flex-wrap items-center gap-3">
        <p className="text-xs text-muted-foreground">
          Showing {formatCount(range.start)}–{formatCount(range.end)} of{" "}
          {formatCount(page.totalItems)} {pluralize(page.totalItems, itemLabel)}
        </p>

        <Select
          value={String(pageSize)}
          onValueChange={(value) => {
            onPageSizeChange(Number(value))
          }}
        >
          <SelectTrigger size="sm" className="h-7 min-w-[7.5rem]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {pageSizeOptions.map((option) => (
              <SelectItem key={option} value={String(option)}>
                {option} per page
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!canGoBack}
          onClick={() => {
            onPageChange(currentPage - 1)
          }}
        >
          <ChevronLeft className="size-3.5" />
          Previous
        </Button>

        {page.totalPages > 1 ? (
          <div className="flex items-center gap-0.5">
            {pageNumbers.map((pageNumber, index) =>
              pageNumber === "ellipsis" ? (
                <span
                  key={`ellipsis-${index}`}
                  className="px-1 text-xs text-muted-foreground"
                  aria-hidden
                >
                  …
                </span>
              ) : (
                <Button
                  key={pageNumber}
                  type="button"
                  variant={pageNumber === currentPage ? "secondary" : "ghost"}
                  size="icon-sm"
                  className="min-w-8"
                  aria-label={`Page ${pageNumber}`}
                  aria-current={pageNumber === currentPage ? "page" : undefined}
                  onClick={() => {
                    onPageChange(pageNumber)
                  }}
                >
                  {pageNumber}
                </Button>
              )
            )}
          </div>
        ) : null}

        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!canGoForward}
          onClick={() => {
            onPageChange(currentPage + 1)
          }}
        >
          Next
          <ChevronRight className="size-3.5" />
        </Button>
      </div>
    </nav>
  )
}

export { Pagination }
