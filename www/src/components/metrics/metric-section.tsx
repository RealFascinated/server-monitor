import type { LucideIcon } from "lucide-react"
import { memo } from "react"
import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

type MetricSectionProps = {
  id: string
  title: string
  sectionKind?: string
  icon: LucideIcon
  description?: string
  render: () => ReactNode
}

function MetricSection({
  id,
  title,
  sectionKind,
  icon: Icon,
  description,
  render,
}: MetricSectionProps) {
  return (
    <section
      data-section-id={id}
      className={cn(
        "flex scroll-mt-[calc(var(--metrics-header-offset)+1rem)] flex-col gap-4"
      )}
    >
      <div className="flex flex-col gap-1 border-b border-border pb-2">
        <div className="flex items-center gap-2.5">
          <h2 className="flex items-center gap-2 text-lg text-foreground">
            <Icon
              className="size-4 shrink-0 text-muted-foreground"
              aria-hidden
            />
            {sectionKind ? (
              <>
                <span className="font-semibold text-muted-foreground">
                  {sectionKind}
                </span>
                <span
                  className="font-normal text-muted-foreground/50"
                  aria-hidden
                >
                  /
                </span>
                <span className="font-bold">{title}</span>
              </>
            ) : (
              <span className="font-bold">{title}</span>
            )}
          </h2>
        </div>
        {description ? (
          <p className="text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      <div className="w-full">{render()}</div>
    </section>
  )
}

const MemoizedMetricSection = memo(MetricSection)

export { MemoizedMetricSection as MetricSection }
