import "@tanstack/table-core"

import type { ServerResponse } from "@/lib/api/user/servers"

declare module "@tanstack/table-core" {
  interface ColumnMeta<TFeatures, TData, TValue> {
    className?: string
    renderServer?: (server: ServerResponse) => React.ReactNode
  }
}
