import "@tanstack/react-table"

import type { ServerResponse } from "@/lib/api/user/servers"

declare module "@tanstack/react-table" {
  interface ColumnMeta<TData, TValue> {
    className?: string
    renderServer?: (server: ServerResponse) => React.ReactNode
  }
}
