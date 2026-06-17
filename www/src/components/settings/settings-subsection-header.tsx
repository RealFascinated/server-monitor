import type { ReactNode } from "react"

type SettingsSubsectionHeaderProps = {
  title: string
  description?: string
  action?: ReactNode
}

function SettingsSubsectionHeader({
  title,
  description,
  action,
}: SettingsSubsectionHeaderProps) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div className="flex min-w-0 flex-col gap-0.5">
        <h3 className="text-sm font-semibold">{title}</h3>
        {description ? (
          <p className="text-xs text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  )
}

export { SettingsSubsectionHeader }
