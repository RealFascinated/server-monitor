import { cn } from "@/lib/utils"

export function MonitorLogo({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      className={cn("size-6 shrink-0", className)}
    >
      <path
        className="fill-monitor"
        d="M3 4h5.5v16H3V4zm12.5 0H21v16h-5.5V4zM8.5 4L12 14.5 15.5 4H19L12.5 20h-1L5 4h3.5z"
      />
    </svg>
  )
}
