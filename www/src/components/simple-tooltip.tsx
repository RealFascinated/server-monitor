import type { ReactElement, ReactNode } from "react"

import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"

type SimpleTooltipProps = {
  content: ReactNode
  children: ReactElement
  side?: "top" | "right" | "bottom" | "left"
  className?: string
}

function SimpleTooltip({
  content,
  children,
  side = "top",
  className,
}: SimpleTooltipProps) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      <TooltipContent side={side} className={className}>
        {content}
      </TooltipContent>
    </Tooltip>
  )
}

function TableHeaderTooltip({
  label,
  tooltip,
}: {
  label: string
  tooltip: string
}) {
  return (
    <SimpleTooltip content={tooltip}>
      <span className="cursor-help">{label}</span>
    </SimpleTooltip>
  )
}

export { SimpleTooltip, TableHeaderTooltip }
