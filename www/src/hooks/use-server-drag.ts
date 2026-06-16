import { useCallback } from "react"
import type { DragEvent } from "react"

import { beginServerDrag } from "@/lib/servers/drag"

type UseServerDragOptions = {
  onDragStart?: () => void
  onDragEnd?: () => void
}

export function useServerDrag(
  serverId: number,
  { onDragStart, onDragEnd }: UseServerDragOptions = {}
) {
  const handleDragStart = useCallback(
    (event: DragEvent<HTMLElement>) => {
      beginServerDrag(event, serverId)
      onDragStart?.()
    },
    [serverId, onDragStart]
  )

  const handleDragEnd = useCallback(() => {
    onDragEnd?.()
  }, [onDragEnd])

  return { onDragStart: handleDragStart, onDragEnd: handleDragEnd }
}
