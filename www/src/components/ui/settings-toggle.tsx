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
        "relative h-5 w-9 shrink-0 rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-monitor focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none disabled:cursor-not-allowed dark:focus-visible:ring-warning dark:focus-visible:ring-offset-base",
        checked
          ? "bg-monitor disabled:bg-monitor/70 dark:bg-warning dark:disabled:bg-warning/70"
          : "bg-muted ring-1 ring-inset ring-border disabled:bg-muted/80 dark:bg-muted-foreground/30 dark:ring-border dark:disabled:bg-muted-foreground/20"
      )}
    >
      <span
        className={cn(
          "absolute top-0.5 left-0.5 size-4 rounded-full shadow-sm transition-transform",
          checked
            ? "translate-x-4 bg-card dark:bg-background"
            : "bg-card dark:bg-foreground",
          disabled && "opacity-80"
        )}
      />
    </button>
  )
}

export { SettingsToggle }
