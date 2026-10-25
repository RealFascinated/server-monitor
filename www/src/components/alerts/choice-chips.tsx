import { cn } from "@/lib/utils"

type ChoiceChipsProps<T extends string | number> = {
  label: string
  value: T
  options: readonly { value: T; label: string }[]
  disabled?: boolean
  onChange: (value: T) => void
}

/** A small single-choice control for short option lists, quicker to scan than a dropdown. */
function ChoiceChips<T extends string | number>({
  label,
  value,
  options,
  disabled,
  onChange,
}: ChoiceChipsProps<T>) {
  return (
    <div
      role="radiogroup"
      aria-label={label}
      className="flex flex-wrap gap-1.5"
    >
      {options.map((option) => {
        const selected = option.value === value

        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            disabled={disabled}
            onClick={() => onChange(option.value)}
            className={cn(
              "rounded-full border px-3 py-1 text-sm transition-colors focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50",
              selected
                ? "border-primary bg-primary/10 font-medium text-brand"
                : "border-border bg-card text-muted-foreground hover:text-foreground"
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

export { ChoiceChips }
