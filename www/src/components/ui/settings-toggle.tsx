import { cn } from "@/lib/utils"

type SettingsToggleProps = {
  checked: boolean
  disabled?: boolean
  ariaLabel: string
  onCheckedChange: (checked: boolean) => void
}

function SettingsToggle({
  checked,
  disabled = false,
  ariaLabel,
  onCheckedChange,
}: SettingsToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        "relative h-5 w-9 shrink-0 rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-monitor focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50 dark:focus-visible:ring-warning dark:focus-visible:ring-offset-base",
        checked
          ? "bg-monitor dark:bg-warning"
          : "bg-muted"
      )}
    >
      <span
        className={cn(
          "absolute top-0.5 left-0.5 size-4 rounded-full bg-card shadow-sm transition-transform",
          checked && "translate-x-4"
        )}
      />
    </button>
  )
}

export { SettingsToggle }
