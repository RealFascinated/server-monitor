import { flexRender } from "@tanstack/react-table"
import type { Row, Table as TanStackTable } from "@tanstack/react-table"
import { ArrowDown, ArrowUp, ArrowUpDown, GripVertical } from "lucide-react"
import type { ReactNode } from "react"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { beginServerDrag } from "@/lib/servers/drag"
import { cn } from "@/lib/utils"

type RowDragConfig<TData> = {
  draggingRowId: string | null
  getServerId: (row: Row<TData>) => number
  getServerLabel: (row: Row<TData>) => string
  onDragStart: (rowId: string) => void
  onDragEnd: () => void
}

type DataTableProps<TData> = {
  table: TanStackTable<TData>
  rowDrag?: RowDragConfig<TData>
  renderRowCells?: (row: Row<TData>) => ReactNode
  renderRow?: (row: Row<TData>) => ReactNode
}

function SortIndicator({ direction }: { direction: false | "asc" | "desc" }) {
  if (direction === "asc") {
    return <ArrowUp className="size-3.5 shrink-0" aria-hidden />
  }

  if (direction === "desc") {
    return <ArrowDown className="size-3.5 shrink-0" aria-hidden />
  }

  return <ArrowUpDown className="size-3.5 shrink-0 opacity-40" aria-hidden />
}

function DataTable<TData>({
  table,
  rowDrag,
  renderRowCells,
  renderRow,
}: DataTableProps<TData>) {
  return (
    <Table>
      <TableHeader>
        {table.getHeaderGroups().map((headerGroup) => (
          <TableRow key={headerGroup.id}>
            {rowDrag ? (
              <TableHead className="w-0 px-2">
                <span className="sr-only">Move</span>
              </TableHead>
            ) : null}
            {headerGroup.headers.map((header) => {
              const canSort = header.column.getCanSort()
              const sortDirection = header.column.getIsSorted()

              return (
                <TableHead
                  key={header.id}
                  className={header.column.columnDef.meta?.className}
                  aria-sort={
                    sortDirection === "asc"
                      ? "ascending"
                      : sortDirection === "desc"
                        ? "descending"
                        : canSort
                          ? "none"
                          : undefined
                  }
                >
                  {header.isPlaceholder ? null : canSort ? (
                    <button
                      type="button"
                      className={cn(
                        "-mx-1 flex items-center gap-1.5 px-1 hover:text-foreground",
                        sortDirection && "text-foreground"
                      )}
                      onClick={header.column.getToggleSortingHandler()}
                    >
                      {flexRender(
                        header.column.columnDef.header,
                        header.getContext()
                      )}
                      <SortIndicator direction={sortDirection} />
                    </button>
                  ) : (
                    flexRender(
                      header.column.columnDef.header,
                      header.getContext()
                    )
                  )}
                </TableHead>
              )
            })}
          </TableRow>
        ))}
      </TableHeader>
      <TableBody>
        {table.getRowModel().rows.map((row) =>
          renderRow ? (
            renderRow(row)
          ) : (
            <TableRow
              key={row.id}
              className={cn(rowDrag?.draggingRowId === row.id && "opacity-40")}
            >
              {rowDrag ? (
                <TableCell className="w-0 px-2">
                  <button
                    type="button"
                    draggable
                    aria-label={`Move ${rowDrag.getServerLabel(row)}`}
                    className="flex cursor-grab items-center text-muted-foreground hover:text-muted-foreground active:cursor-grabbing dark:hover:text-foreground"
                    onDragStart={(event) => {
                      beginServerDrag(event, rowDrag.getServerId(row))
                      rowDrag.onDragStart(row.id)
                    }}
                    onDragEnd={rowDrag.onDragEnd}
                  >
                    <GripVertical className="size-4" aria-hidden />
                  </button>
                </TableCell>
              ) : null}
              {renderRowCells
                ? renderRowCells(row)
                : row.getVisibleCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      className={cell.column.columnDef.meta?.className}
                    >
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
            </TableRow>
          )
        )}
      </TableBody>
    </Table>
  )
}

export { DataTable }
