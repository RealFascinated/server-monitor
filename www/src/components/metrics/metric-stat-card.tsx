import { CountUp } from "@/components/count-up"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Card, CardContent } from "@/components/ui/card"
import { cn } from "@/lib/utils"

type MetricStatCardProps = {
  title: string
  value: number
  formatValue: (value: number) => string
  detail?: string
  detailTooltip?: string
  valueClassName?: string
  animate?: boolean
}

function MetricStatCard({
  title,
  value,
  formatValue,
  detail,
  detailTooltip,
  valueClassName,
  animate = true,
}: MetricStatCardProps) {
  const valueClass = cn(
    "font-mono text-3xl font-semibold text-foreground tabular-nums",
    valueClassName
  )

  return (
    <Card className="gap-0 overflow-hidden py-0">
      <CardContent className="flex flex-col gap-1 px-4 py-3">
        <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
          {title}
        </p>
        {animate ? (
          <CountUp value={value} format={formatValue} className={valueClass} />
        ) : (
          <span className={valueClass}>{formatValue(value)}</span>
        )}
        {detail ? (
          detailTooltip ? (
            <SimpleTooltip content={detailTooltip}>
              <p className="cursor-help text-sm text-muted-foreground">
                {detail}
              </p>
            </SimpleTooltip>
          ) : (
            <p className="text-sm text-muted-foreground">{detail}</p>
          )
        ) : null}
      </CardContent>
    </Card>
  )
}

export { MetricStatCard }
