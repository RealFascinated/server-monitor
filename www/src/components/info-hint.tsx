import { Info } from "lucide-react"
import { useState } from "react"

import { Button } from "@/components/ui/button"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

type InfoHintProps = {
  /** Accessible name, e.g. "About CPU breakdown". */
  label: string
  children: string
}

/**
 * Small info icon that reveals help text. Opens on hover, and on click/tap/
 * keyboard so it also works on touch devices.
 */
function InfoHint({ label, children }: InfoHintProps) {
  const [open, setOpen] = useState(false)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-xs"
          className="shrink-0 text-muted-foreground"
          aria-label={label}
          onMouseEnter={() => setOpen(true)}
          onMouseLeave={() => setOpen(false)}
        >
          <Info />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-72 p-3 text-xs leading-relaxed text-popover-foreground"
        onOpenAutoFocus={(event) => event.preventDefault()}
      >
        {children}
      </PopoverContent>
    </Popover>
  )
}

export { InfoHint }
