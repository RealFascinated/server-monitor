import { Search } from "lucide-react"

import { Input } from "@/components/ui/input"

export function SidebarSearch({
  value,
  onChange,
}: {
  value: string
  onChange: (value: string) => void
}) {
  return (
    <>
      <div className="mt-3 mb-0 shrink-0 px-2">
        <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
          Servers
        </p>
      </div>
      <div className="relative my-2 shrink-0 px-2">
        <Search
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-4 size-3.5 -translate-y-1/2 text-muted-foreground"
        />
        <Input
          type="search"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Search servers…"
          aria-label="Search servers"
          className="h-7 pl-8 text-xs"
        />
      </div>
    </>
  )
}
