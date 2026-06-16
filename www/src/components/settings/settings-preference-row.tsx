import type { ReactNode } from "react"

type SettingsPreferenceRowProps = {
  label: string
  description: string
  control: ReactNode
}

function SettingsPreferenceRow({
  label,
  description,
  control,
}: SettingsPreferenceRowProps) {
  return (
    <div className="flex max-w-xl items-start justify-between gap-8">
      <div className="flex flex-col gap-1.5">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs font-bold text-muted-foreground">{description}</p>
      </div>

      <div className="shrink-0 pt-0.5">{control}</div>
    </div>
  )
}

export { SettingsPreferenceRow }
