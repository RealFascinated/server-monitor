import type { ReactNode } from "react"

import { LoadingState } from "@/components/loading-state"
import { cn } from "@/lib/utils"

type AnimatedContentProps = {
  children: ReactNode
  className?: string
}

function AnimatedContent({ children, className }: AnimatedContentProps) {
  return <div className={cn("motion-enter", className)}>{children}</div>
}

type AsyncContentProps = {
  loading: boolean
  loadingMessage?: string
  children: ReactNode
  className?: string
  centered?: boolean
}

function AsyncContent({
  loading,
  loadingMessage,
  children,
  className,
  centered,
}: AsyncContentProps) {
  if (loading) {
    return (
      <LoadingState
        message={loadingMessage}
        className={className}
        centered={centered}
      />
    )
  }

  return <AnimatedContent className={className}>{children}</AnimatedContent>
}

export { AnimatedContent, AsyncContent }
